# Hướng dẫn triển khai dự án Java multi-module theo Clean Architecture / Hexagonal (Gradle Groovy DSL)

## 1. Mục tiêu dự án

Xây dựng một project Java theo mô hình **Gradle multi-modules** với các yêu cầu:

- Có ít nhất 3 module domain: `user`, `booking`, `ticket`
- Có 1 module để chạy ứng dụng: `app`
- Tích hợp:
  - PostgreSQL
  - Redis
  - RabbitMQ
  - Nginx
- Chạy được bằng Docker Compose
- Áp dụng kiến trúc:
  - Clean Architecture
  - Hexagonal Architecture
- Có unit test cho các function tự tạo
- Thử tích hợp Testcontainers để test e2e cho một số API quan trọng

---

## 2. Tech stack

| Technology | Version |
|------------|---------|
| Java | 25 LTS (Eclipse Temurin) |
| Spring Boot | 4.0.1 |
| PostgreSQL | 18 |
| Redis | 8.4 |
| Nginx | 1.28.1 |
| Gradle | 9.2.1 |

---

## 3. Cấu trúc tổng thể project

```text
booking-platform/
├─ build.gradle
├─ settings.gradle
├─ gradle/
│  └─ wrapper/
├─ gradlew
├─ gradlew.bat
├─ compose.yaml
├─ Dockerfile
├─ docker/
│  └─ nginx/
│     └─ nginx.conf
├─ shared-kernel/
├─ user/
├─ booking/
├─ ticket/
└─ app/
```

---

## 4. Ý nghĩa từng module

### 4.1 `shared-kernel`
Chứa những thành phần dùng chung cho toàn hệ thống:

- base exception
- base domain event
- result wrapper
- shared constants
- common utils
- clock abstraction
- common response object

> Lưu ý: chỉ để những gì thực sự dùng chung. Không biến module này thành “thùng rác”.

### 4.2 `user`
Quản lý nghiệp vụ liên quan đến người dùng:

- tạo user
- xem thông tin user
- cập nhật user

### 4.3 `booking`
Quản lý nghiệp vụ đặt chỗ:

- tạo booking
- xác nhận booking
- hủy booking

### 4.4 `ticket`
Quản lý nghiệp vụ vé:

- phát hành ticket từ booking đã xác nhận
- xem ticket
- đánh dấu ticket đã sử dụng

### 4.5 `app`
Module để chạy ứng dụng Spring Boot:

- chứa class `main`
- cấu hình Spring Boot
- cấu hình datasource, redis, rabbitmq
- wiring các module lại với nhau

> Không đặt business logic trong `app`.

---

## 5. Cấu trúc thư mục chi tiết theo từng module domain

Ví dụ với module `booking`:

```text
booking/
├─ build.gradle
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ com/example/booking/
   │  │     ├─ domain/
   │  │     │  ├─ model/
   │  │     │  ├─ service/
   │  │     │  ├─ event/
   │  │     │  └─ exception/
   │  │     ├─ application/
   │  │     │  ├─ port/
   │  │     │  │  ├─ in/
   │  │     │  │  └─ out/
   │  │     │  ├─ usecase/
   │  │     │  └─ service/
   │  │     ├─ adapter/
   │  │     │  ├─ in/
   │  │     │  │  ├─ web/
   │  │     │  │  └─ messaging/
   │  │     │  └─ out/
   │  │     │     ├─ persistence/
   │  │     │     ├─ cache/
   │  │     │     └─ messaging/
   │  │     └─ config/
   │  └─ resources/
   └─ test/
      └─ java/
```

### Mapping Clean Architecture / Hexagonal

- `domain/`
  - entity
  - value object
  - domain service
  - domain event
  - business rule

- `application/port/in`
  - input ports
  - interface của use case

- `application/port/out`
  - output ports
  - interface cho repository, publisher, cache, external service

- `application/service`
  - triển khai use case
  - orchestration nghiệp vụ

- `adapter/in/web`
  - REST controller

- `adapter/in/messaging`
  - RabbitMQ listener

- `adapter/out/persistence`
  - JPA entity
  - Spring Data repository
  - mapper domain <-> entity

- `adapter/out/cache`
  - Redis cache adapter

- `adapter/out/messaging`
  - RabbitMQ publisher

---

## 6. Quy tắc phụ thuộc giữa các layer

Phải tuân thủ các nguyên tắc sau:

1. `domain` không được phụ thuộc Spring, JPA, Redis, RabbitMQ
2. `application` chỉ phụ thuộc `domain` và `port`
3. `adapter` phụ thuộc `application`
4. `app` phụ thuộc các domain modules
5. Không để domain module phụ thuộc vòng tròn vào nhau
6. Giao tiếp giữa các module ưu tiên:
   - sync qua `port`
   - async qua event RabbitMQ

---

## 7. Cấu hình Gradle multi-modules bằng Groovy DSL

## 7.1 `settings.gradle`

```groovy
rootProject.name = 'booking-platform'

include 'shared-kernel', 'user', 'booking', 'ticket', 'app'
```

## 7.2 Root `build.gradle`

```groovy
plugins {
    id 'jacoco'
    id 'org.springframework.boot' version '4.0.1' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

allprojects {
    group = 'com.example'
    version = '0.0.1-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java-library'
    apply plugin: 'jacoco'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    test {
        useJUnitPlatform()
    }
}
```

## 7.3 `shared-kernel/build.gradle`

```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.13.4'
}
```

## 7.4 `user/build.gradle`

```groovy
dependencies {
    implementation project(':shared-kernel')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.postgresql:postgresql'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 7.5 `booking/build.gradle`

```groovy
dependencies {
    implementation project(':shared-kernel')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.postgresql:postgresql'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 7.6 `ticket/build.gradle`

```groovy
dependencies {
    implementation project(':shared-kernel')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.postgresql:postgresql'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 7.7 `app/build.gradle`

```groovy
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
    id 'java'
}

dependencies {
    implementation project(':shared-kernel')
    implementation project(':user')
    implementation project(':booking')
    implementation project(':ticket')

    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.21.3'
    testImplementation 'org.testcontainers:postgresql:1.21.3'
    testImplementation 'org.testcontainers:rabbitmq:1.21.3'
    testImplementation 'org.testcontainers:testcontainers:1.21.3'
}
```

---

## 8. Cấu trúc module `app`

```text
app/
├─ build.gradle
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ com/example/app/
   │  │     ├─ BookingPlatformApplication.java
   │  │     └─ config/
   │  └─ resources/
   │     ├─ application.yml
   │     ├─ application-local.yml
   │     ├─ application-docker.yml
   │     └─ application-test.yml
   └─ test/
      └─ java/
```

### Nhiệm vụ của `app`

- bootstrap ứng dụng
- cấu hình profile
- import bean/config
- chạy toàn bộ hệ thống

---

## 9. Tích hợp PostgreSQL, Redis, RabbitMQ

## 9.1 PostgreSQL
Dùng làm database chính cho hệ thống.

### Gợi ý
- `users`
- `bookings`
- `tickets`

Mỗi domain quản lý aggregate riêng, không truy cập trực tiếp bảng của nhau nếu không cần.

## 9.2 Redis
Dùng cho caching các API đọc nhiều:

- `userById`
- `bookingById`
- `ticketById`

### Quy tắc
- API đọc thì cache
- API ghi thì evict cache
- TTL ngắn, ví dụ 5–10 phút

## 9.3 RabbitMQ
Dùng để giao tiếp async giữa các module.

### Ví dụ event flow
1. Booking được xác nhận
2. Publish event `BookingConfirmed`
3. Module `ticket` consume event
4. Tạo ticket
5. Publish tiếp `TicketIssued`

---

## 10. Dockerfile

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .
RUN ./gradlew clean test :app:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/app/build/libs/app-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## 11. Docker Compose

## 11.1 `compose.yaml`

```yaml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: booking-platform/app:local
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bookingdb
      SPRING_DATASOURCE_USERNAME: booking
      SPRING_DATASOURCE_PASSWORD: booking
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    expose:
      - "8080"
    networks:
      - backend

  nginx:
    image: nginx:1.28.1
    ports:
      - "80:80"
    volumes:
      - ./docker/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app
    networks:
      - backend

  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: bookingdb
      POSTGRES_USER: booking
      POSTGRES_PASSWORD: booking
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U booking -d bookingdb"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - backend

  redis:
    image: redis:8.4
    command: ["redis-server", "--appendonly", "yes"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - backend

  rabbitmq:
    image: rabbitmq:4-management
    ports:
      - "15672:15672"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - backend

volumes:
  postgres_data:

networks:
  backend:
```

## 11.2 `docker/nginx/nginx.conf`

```nginx
events {}

http {
  upstream app_upstream {
    server app:8080;
  }

  server {
    listen 80;

    location / {
      proxy_pass http://app_upstream;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }
  }
}
```

---

## 12. Trình tự triển khai đề xuất

## Giai đoạn 1 - Khởi tạo project
1. Tạo root project Gradle
2. Tạo các module:
   - `shared-kernel`
   - `user`
   - `booking`
   - `ticket`
   - `app`
3. Cấu hình `settings.gradle`
4. Cấu hình root `build.gradle`
5. Tạo package structure chuẩn cho từng module

## Giai đoạn 2 - Xây dựng domain
1. Xác định entity, value object, business rules
2. Tạo input ports
3. Tạo output ports
4. Viết use case service
5. Viết controller
6. Viết persistence adapter
7. Viết cache adapter
8. Viết message publisher/listener

## Giai đoạn 3 - Tích hợp hạ tầng
1. Cấu hình PostgreSQL
2. Cấu hình Redis cache
3. Cấu hình RabbitMQ exchange/queue/routing key
4. Cấu hình profile `local`, `docker`, `test`

## Giai đoạn 4 - Test
1. Viết unit test cho:
   - domain service
   - use case
   - validation
   - mapper
2. Viết integration test cho:
   - controller
   - repository
3. Viết e2e test với Testcontainers cho API quan trọng

## Giai đoạn 5 - Docker hóa
1. Viết `Dockerfile`
2. Viết `compose.yaml`
3. Viết cấu hình Nginx
4. Chạy local bằng Docker Compose

---

## 13. Chiến lược test

## 13.1 Unit test
Mỗi function tự viết cần có test cho:
- happy path
- invalid input
- business rule fail
- edge case nếu có

### Nên test các lớp sau
- domain service
- use case service
- validator
- mapper
- utility class

## 13.2 Integration test
Test các thành phần tích hợp:
- REST API
- repository
- Redis cache behavior
- RabbitMQ publisher/listener

## 13.3 E2E test với Testcontainers
Nên chọn vài API quan trọng:

- `POST /users`
- `POST /bookings`
- `POST /bookings/{id}/confirm`
- `GET /tickets/{id}`

### Mục tiêu
- chạy với Postgres thật
- chạy với Redis thật
- chạy với RabbitMQ thật
- verify end-to-end flow

---

## 14. Quy ước code để giữ kiến trúc sạch

1. Không import Spring vào `domain`
2. Không đặt business logic trong controller
3. Controller chỉ gọi `port.in`
4. Persistence adapter implement `port.out`
5. RabbitMQ publisher chỉ là adapter, không chứa business logic
6. Redis chỉ là kỹ thuật cache, không chứa rule nghiệp vụ
7. Không cho module này truy cập adapter nội bộ của module khác

---

## 15. Gợi ý API nghiệp vụ mẫu

## User
- `POST /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`

## Booking
- `POST /api/bookings`
- `POST /api/bookings/{id}/confirm`
- `POST /api/bookings/{id}/cancel`
- `GET /api/bookings/{id}`

## Ticket
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/use`

---

## 16. Lệnh làm việc cơ bản

### Build và test
```bash
./gradlew clean test
```

### Chạy app local
```bash
./gradlew :app:bootRun
```

### Build jar
```bash
./gradlew :app:bootJar
```

### Chạy Docker Compose
```bash
docker compose up --build
```

### Tắt hệ thống
```bash
docker compose down
```

---

## 17. Định nghĩa hoàn thành đề xuất

Một chức năng được coi là hoàn thành khi:

- code đúng theo package/layer quy định
- có unit test
- pass toàn bộ test
- tích hợp đúng DB/cache/queue nếu cần
- không vi phạm dependency rule
- chạy được bằng Docker Compose

---

## 18. Kết luận

Cấu trúc đề xuất ở trên phù hợp để:

- dễ phát triển theo team
- rõ ràng trách nhiệm từng module
- bám sát Clean Architecture / Hexagonal
- dễ mở rộng thêm domain mới
- dễ tích hợp CI/CD, monitoring, migration sau này

Nếu muốn mở rộng tiếp, có thể bổ sung:

- Flyway hoặc Liquibase
- OpenAPI / Swagger
- ArchUnit để kiểm tra kiến trúc
- JaCoCo coverage rule
- CI pipeline
- centralized exception handling
- observability với Micrometer + Prometheus
