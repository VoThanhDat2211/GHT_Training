# Giải thích cấu trúc project

## Tổng quan cây thư mục

```
multi-module/
├── gradlew / gradlew.bat
├── settings.gradle
├── build.gradle
├── Dockerfile
├── docker-compose.yml
├── .gitignore
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── docker/
│   └── nginx/
│       └── nginx.conf
│
├── common/
├── user/
├── booking/
├── ticket/
├── app/
│
└── knowledge-base/
```

---

## Root project — các file ở thư mục gốc

### `settings.gradle`
Khai báo tên project gốc và toàn bộ submodule tham gia vào build.
Gradle đọc file này đầu tiên để biết project có bao nhiêu module.
```groovy
rootProject.name = 'multi-module'
include 'common'
include 'user'
...
```
Không có file này thì Gradle không biết các module con tồn tại.

---

### `build.gradle` (root)
Cấu hình áp dụng chung cho **tất cả subproject** thông qua block `subprojects { }`.
- Khai báo plugin Spring Boot và Spring Dependency Management với `apply false` — tức là đăng ký plugin nhưng **chưa áp dụng** cho root, chỉ để submodule dùng khi cần.
- Áp dụng `java` plugin cho tất cả submodule.
- Cấu hình Java Toolchain Java 25 cho tất cả.
- Import Spring Boot BOM qua `dependencyManagement` — nhờ đó các module con **không cần khai báo version** của Spring dependency.
- Khai báo Lombok và JUnit là dependency chung.

---

### `gradlew` / `gradlew.bat`
Script khởi động Gradle Wrapper.
- `gradlew` — dùng trên Linux/macOS
- `gradlew.bat` — dùng trên Windows

Wrapper cho phép chạy đúng version Gradle đã chỉ định mà **không cần cài Gradle trên máy**. Khi chạy lần đầu, Wrapper tự download Gradle về `~/.gradle/wrapper/dists/`.

Lệnh thường dùng:
```bash
.\gradlew.bat build              # Build toàn bộ project
.\gradlew.bat :user:compileJava  # Compile riêng module user
.\gradlew.bat test               # Chạy tất cả unit test
.\gradlew.bat :app:bootJar       # Tạo executable JAR từ module app
```

---

### `Dockerfile`
Định nghĩa cách build Docker image cho ứng dụng. Dùng **multi-stage build** để tối ưu kích thước image:

- **Stage 1 (builder):** Dùng JDK 25 để compile và build file JAR từ source.
- **Stage 2 (runtime):** Chỉ dùng JRE 25 (nhẹ hơn JDK) để chạy file JAR.

Kết quả: image cuối chỉ chứa JRE + file JAR, không có source code hay Gradle.

---

### `docker-compose.yml`
Định nghĩa và kết nối tất cả service cần thiết để chạy ứng dụng:

| Service | Image | Port | Mục đích |
|---------|-------|------|----------|
| `postgres` | postgres:18 | 5432 | Database chính |
| `redis` | redis:8.4-alpine | 6379 | Cache |
| `rabbitmq` | rabbitmq:4-management-alpine | 5672, 15672 | Message Queue |
| `app` | build từ Dockerfile | 8080 | Spring Boot app |
| `nginx` | nginx:1.28.1-alpine | 80 | Reverse proxy |

Tất cả service kết nối qua `app-network`. Service `app` chỉ khởi động sau khi postgres, redis, rabbitmq đã `healthy` (nhờ `depends_on` + `condition: service_healthy`).

Lệnh chạy:
```bash
docker compose up --build -d   # Build image và chạy nền
docker compose logs -f app     # Xem log app
docker compose down -v         # Dừng và xóa volume
```

---

### `.gitignore`
Liệt kê các file/thư mục không được commit lên Git:
- `.gradle/` — cache của Gradle
- `build/` — output build (bytecode, JAR)
- `.idea/` — cấu hình IDE IntelliJ

---

## Thư mục `gradle/`

### `gradle/libs.versions.toml` — Version Catalog
File trung tâm quản lý **tất cả version dependency** của project.
Chia làm 3 section:
- `[versions]` — khai báo tên alias → version string
- `[libraries]` — khai báo tên alias → group:artifact, tham chiếu version từ section trên
- `[plugins]` — khai báo Gradle plugin

Lợi ích: chỉ cần sửa version ở 1 chỗ duy nhất, toàn bộ module tự dùng version mới. IDE cũng có autocomplete khi dùng alias trong `build.gradle`.

### `gradle/wrapper/gradle-wrapper.properties`
Cấu hình Gradle Wrapper:
- `distributionUrl` — URL download Gradle, chỉ định version 9.2.1
- `distributionBase` / `zipStoreBase` — nơi lưu bản download (~/.gradle)

### `gradle/wrapper/gradle-wrapper.jar`
File JAR binary nhỏ (~45KB). Đây là bootstrap loader — khi chạy `gradlew`, script gọi vào JAR này để tải đúng version Gradle từ URL trong `.properties`. File này phải được commit vào Git.

---

## Thư mục `docker/`

### `docker/nginx/nginx.conf`
Cấu hình Nginx làm reverse proxy:
- Nhận request tại port 80
- Forward `/api/*` và `/swagger-ui/*` đến `app:8080`
- Bật gzip compression cho JSON response
- Rate limiting: 20 request/giây per IP, burst 40

---

## Module `common`

**Vai trò:** Chứa các building block thuần Java dùng chung, không phụ thuộc domain nào và không import Spring.

### `common/build.gradle`
Không có dependency nào ngoài Lombok (kế thừa từ root). `common` là module nền tảng nhất, không phụ thuộc module nào khác.

### `BaseId<T>`
Abstract class generic cho tất cả ID của các entity.
- Buộc mọi ID phải wrap một value (UUID, Long...) thay vì dùng raw type.
- Implement `equals` và `hashCode` dựa trên value → so sánh ID an toàn.

```
UserId extends BaseId<UUID>
BookingId extends BaseId<UUID>
TicketId extends BaseId<UUID>
```

### `DomainEvent<T>`
Interface đánh dấu tất cả domain event. Mọi event phải implement interface này và khai báo entity liên quan.

### `DomainException`
Base exception cho tất cả domain. Mọi exception nghiệp vụ kế thừa từ class này để có thể bắt chung ở exception handler.

---

## Module `user` — ví dụ chi tiết (booking, ticket tương tự)

### `user/build.gradle`
Khai báo dependency cần cho tất cả layer của domain user:
- `project(':common')` — dùng BaseId, DomainEvent...
- Spring Web, Validation, JPA, Redis, AMQP — cho các adapter layer
- MapStruct — generate code mapping giữa các object
- ArchUnit — viết test kiểm tra quy tắc dependency giữa package

---

### Layer `domain/` — Tầng nghiệp vụ (không import Spring, JPA, Redis)

#### `domain/entity/User.java`
Aggregate Root của domain User.
- Chứa toàn bộ business rule của User (validate name, email, trạng thái hợp lệ).
- Factory method `User.create()` thay vì constructor public — đảm bảo User luôn ở trạng thái hợp lệ khi tạo.
- Method `deactivate()`, `delete()` chứa rule nghiệp vụ (không thể deactivate user đã deleted).
- **Không có annotation Spring hay JPA** — class thuần Java.

#### `domain/valueobject/UserId.java`
Value Object đại diện cho ID của User. Kế thừa `BaseId<UUID>`. Immutable.

#### `domain/valueobject/UserStatus.java`
Enum các trạng thái hợp lệ của User: `ACTIVE`, `INACTIVE`, `DELETED`.

#### `domain/event/UserCreatedEvent.java`
Domain Event phát sinh khi User được tạo. Implement `DomainEvent<User>`. Chứa snapshot thời điểm event xảy ra.

#### `domain/exception/UserDomainException.java`
Exception đặc thù cho User domain. Kế thừa `DomainException`.

#### `domain/port/input/UserApplicationService.java`
**Input Port** — interface định nghĩa những use case mà bên ngoài có thể gọi vào domain User. REST Controller và test sẽ dùng interface này, không dùng implementation trực tiếp.

#### `domain/port/output/UserRepository.java`
**Output Port** — interface định nghĩa cách domain cần persist/query User. Domain chỉ biết interface này, không biết Postgres hay Redis đằng sau.

#### `domain/port/output/UserMessagePublisher.java`
**Output Port** — interface định nghĩa cách domain cần publish event. Domain không biết RabbitMQ hay Kafka.

---

### Layer `application/` — Tầng điều phối use case

#### `application/dto/command/CreateUserCommand.java`
Data Transfer Object đầu vào cho use case tạo User. Dùng Java Record (immutable). Có Bean Validation annotation (`@NotBlank`, `@Email`).

#### `application/dto/response/UserResponse.java`
DTO đầu ra trả về cho caller. Dùng Java Record. Không expose domain entity trực tiếp ra ngoài.

#### `application/mapper/UserDataMapper.java`
Convert giữa Domain Entity và DTO. Spring `@Component`, được inject vào Application Service.

#### `application/service/UserApplicationServiceImpl.java`
Implement Input Port `UserApplicationService`. Đây là nơi điều phối use case:
1. Validate command (email unique)
2. Gọi `User.create()` để tạo entity
3. Persist qua Output Port `UserRepository`
4. Publish event qua Output Port `UserMessagePublisher`
5. Trả về DTO

Chỉ biết interface (port), không biết implementation cụ thể.

---

### Layer `dataaccess/` — Tầng persistence adapter

#### `dataaccess/entity/UserJpaEntity.java`
JPA Entity — đại diện bảng `users` trong PostgreSQL. **Hoàn toàn tách biệt** với Domain Entity `User`. Có JPA annotation (`@Entity`, `@Table`, `@Column`...).

#### `dataaccess/repository/UserJpaRepository.java`
Spring Data JPA Repository interface. Chỉ có ở layer này, domain không biết sự tồn tại của nó.

#### `dataaccess/mapper/UserDataAccessMapper.java`
Convert giữa Domain Entity `User` và JPA Entity `UserJpaEntity`.

#### `dataaccess/adapter/UserRepositoryImpl.java`
**Implement Output Port** `UserRepository`. Đây là cầu nối:
- Nhận Domain Entity từ application layer
- Convert sang JPA Entity
- Gọi JpaRepository để persist
- Convert kết quả ngược lại sang Domain Entity

---

### Layer `messaging/` — Tầng messaging adapter

#### `messaging/publisher/UserCreatedEventPublisher.java`
**Implement Output Port** `UserMessagePublisher`. Dùng `RabbitTemplate` để gửi event lên RabbitMQ exchange `user.events` với routing key `user.created`.

#### `config/UserRabbitMqConfig.java`
Khai báo RabbitMQ topology cho User domain:
- `TopicExchange` — nhận event từ publisher
- `Queue` với Dead Letter Queue (DLQ) — message lỗi sẽ vào DLQ thay vì bị mất
- `Binding` — liên kết queue với exchange qua routing key

---

### Layer `rest/` — Tầng HTTP adapter

#### `rest/UserController.java`
**Primary Adapter** — REST Controller expose HTTP API:
- `POST /api/users` → gọi `UserApplicationService.createUser()`
- `GET /api/users/{id}` → gọi `UserApplicationService.getUserById()`
- `DELETE /api/users/{id}` → gọi `UserApplicationService.deleteUser()`

Controller chỉ biết Input Port interface, không biết implementation.

---

## Module `booking` và `ticket`

Hiện tại chỉ có cấu trúc thư mục (package) đã được tạo sẵn, chưa có code.
Sẽ được implement theo đúng pattern của module `user`:
- `booking` phụ thuộc `user` (booking cần validate user tồn tại)
- `ticket` phụ thuộc `booking` (ticket được phát hành từ booking)

---

## Module `app` — Runner

### `app/build.gradle`
Apply plugin `org.springframework.boot` — chỉ module này build ra executable JAR (`bootJar`).
Import tất cả module domain (`user`, `booking`, `ticket`).
Khai báo TestContainers dependency cho integration test.

### `app/src/main/java/com/multimodule/MultiModuleApplication.java`
Entry point của toàn bộ ứng dụng với `@SpringBootApplication`.
`scanBasePackages = "com.multimodule"` để Spring scan bean từ tất cả module con.

### `app/src/main/resources/application.yml`
Cấu hình runtime của ứng dụng. Tất cả giá trị nhạy cảm đọc từ environment variable với default fallback cho local dev:
```yaml
url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:multimodule_db}
```
Khi chạy trong Docker, các biến môi trường được inject từ `docker-compose.yml`.

### `app/src/test/resources/application-test.yml`
Override config khi chạy test: `ddl-auto: create-drop` để tự tạo/xóa schema, `show-sql: true` để debug.

---

## Quy tắc import giữa các package (bắt buộc tuân theo)

```
domain     → chỉ import: common
application → chỉ import: domain, common
dataaccess  → chỉ import: domain, common
messaging   → chỉ import: domain, common
rest        → chỉ import: application, domain
config      → có thể import bất kỳ layer nào (chỉ để wire DI)
```

Vi phạm quy tắc này sẽ bị phát hiện bởi ArchUnit test trong `src/test/java/.../arch/`.
