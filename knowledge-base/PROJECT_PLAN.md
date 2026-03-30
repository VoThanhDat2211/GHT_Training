# Project Plan — Java Gradle Multi-Module (Hexagonal Architecture)

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 25 LTS (Eclipse Temurin) |
| Spring Boot | 4.0.1 |
| PostgreSQL | 18 |
| Redis | 8.4 |
| RabbitMQ | 4.x (latest stable) |
| Nginx | 1.28.1 |
| Gradle | 9.2.1 |

---

## 1. Cấu trúc dự án (Project Structure)

```
multi-module/
├── settings.gradle                      # Khai báo tất cả submodule
├── build.gradle                         # Root build config (shared plugins, versions)
├── gradle/
│   └── libs.versions.toml              # Version Catalog (toàn bộ dependency + version)
│
├── Dockerfile                           # Build image cho app module
├── docker-compose.yml                   # Chạy toàn bộ stack (app + infra)
├── docker-compose.dev.yml               # Override cho local dev
├── docker/
│   └── nginx/
│       └── nginx.conf                   # Reverse proxy config
│
├── common/                              # Module dùng chung (không phụ thuộc domain)
│   └── src/main/java/com/example/common/
│       ├── domain/
│       │   ├── valueobject/             # BaseId, Money, ...
│       │   └── event/                   # BaseEvent interface
│       └── exception/                   # Base exceptions (DomainException, ...)
│
├── user/                                # Domain: User
│   ├── user-domain/                     # Tầng Domain Core (entity, port, domain service)
│   ├── user-application/               # Tầng Application (use case, DTO, mapper)
│   ├── user-dataaccess/                # Tầng Adapter - Persistence (JPA, Redis)
│   ├── user-messaging/                 # Tầng Adapter - Messaging (RabbitMQ)
│   └── user-container/                 # Tầng Adapter - REST (Controller, Spring config)
│
├── booking/                             # Domain: Booking
│   ├── booking-domain/
│   ├── booking-application/
│   ├── booking-dataaccess/
│   ├── booking-messaging/
│   └── booking-container/
│
├── ticket/                              # Domain: Ticket
│   ├── ticket-domain/
│   ├── ticket-application/
│   ├── ticket-dataaccess/
│   ├── ticket-messaging/
│   └── ticket-container/
│
└── app/                                 # Module runner — Spring Boot main class
    └── src/main/java/com/example/
        └── MultiModuleApplication.java
```

---

## 2. Kiến trúc Hexagonal (Clean Architecture)

### Nguyên tắc cốt lõi

```
┌────────────────────────────────────────────────────────────────┐
│                        DOMAIN CORE                             │
│   Entity · Value Object · Domain Event · Domain Service        │
│         Input Port (interface) · Output Port (interface)       │
└──────────────┬──────────────────────────┬──────────────────────┘
               │                          │
   ┌───────────▼──────────┐  ┌────────────▼───────────────────┐
   │  APPLICATION LAYER   │  │     SECONDARY ADAPTERS         │
   │  (Use Cases)         │  │  - DataAccess (JPA + Redis)    │
   │  Implements:         │  │  - Messaging (RabbitMQ)        │
   │  Input Port          │  │  Implements: Output Port       │
   └───────────┬──────────┘  └────────────────────────────────┘
               │
   ┌───────────▼──────────┐
   │  PRIMARY ADAPTERS    │
   │  - REST Controller   │
   │  - Event Listener    │
   └──────────────────────┘
```

### Quy tắc phụ thuộc (Dependency Rule)

- `user-domain` **không** phụ thuộc bất kỳ module nào khác (chỉ phụ thuộc `common`)
- `user-application` phụ thuộc `user-domain`
- `user-dataaccess` phụ thuộc `user-domain` (implement Output Port)
- `user-messaging` phụ thuộc `user-domain` (implement Output Port)
- `user-container` phụ thuộc tất cả các layer để wire DI
- `app` phụ thuộc tất cả `*-container`

### Cấu trúc từng layer (ví dụ domain `user`)

```
user-domain/src/main/java/com/example/user/domain/
├── entity/
│   └── User.java                        # Root Aggregate
├── valueobject/
│   ├── UserId.java
│   └── UserStatus.java
├── event/
│   └── UserCreatedEvent.java
├── exception/
│   └── UserDomainException.java
├── service/
│   ├── UserDomainService.java           # Interface
│   └── UserDomainServiceImpl.java       # Implementation
└── ports/
    ├── input/
    │   └── service/
    │       └── UserApplicationService.java   # Input Port
    └── output/
        ├── repository/
        │   └── UserRepository.java           # Output Port
        └── message/
            └── UserMessagePublisher.java     # Output Port

user-application/src/main/java/com/example/user/application/
├── dto/
│   ├── command/
│   │   └── CreateUserCommand.java
│   └── response/
│       └── UserResponse.java
├── mapper/
│   └── UserDataMapper.java
├── handler/
│   └── UserCommandHandler.java
└── service/
    └── UserApplicationServiceImpl.java       # Implements Input Port

user-dataaccess/src/main/java/com/example/user/dataaccess/
├── entity/
│   └── UserJpaEntity.java
├── repository/
│   └── UserJpaRepository.java               # Spring Data JPA
├── cache/
│   └── UserCacheAdapter.java                # Redis cache
├── mapper/
│   └── UserDataAccessMapper.java
└── adapter/
    └── UserRepositoryImpl.java              # Implements Output Port

user-messaging/src/main/java/com/example/user/messaging/
├── publisher/
│   └── UserCreatedEventPublisher.java       # Implements Output Port
├── listener/
│   └── UserEventListener.java
└── mapper/
    └── UserMessagingMapper.java

user-container/src/main/java/com/example/user/
├── rest/
│   └── UserController.java                  # REST Adapter (Primary)
├── config/
│   └── UserBeanConfig.java                  # Spring @Bean wiring
└── exception/
    └── UserGlobalExceptionHandler.java
```

---

## 3. Các bước thực hiện (Implementation Phases)

### Phase 1 — Khởi tạo dự án Gradle Multi-Module

**Mục tiêu:** Dựng skeleton project, đảm bảo build thành công trước khi code nghiệp vụ.

- [ ] Tạo thư mục gốc `multi-module/`
- [ ] Khởi tạo `settings.gradle` — khai báo tên project và include tất cả submodule
- [ ] Tạo `gradle/libs.versions.toml` — Version Catalog cho toàn bộ dependency
  - Java 25, Spring Boot 4.0.1, Spring Data JPA, Spring Data Redis
  - Spring AMQP (RabbitMQ), PostgreSQL Driver, Lombok, MapStruct
  - JUnit 5, Mockito, TestContainers
- [ ] Tạo root `build.gradle` — apply plugin chung, java toolchain Java 25
- [ ] Tạo từng submodule với `build.gradle` riêng (chỉ khai báo dependency của layer đó)
- [ ] Chạy `./gradlew build` — đảm bảo toàn bộ module compile sạch

**Output:** Project build được, chưa có logic.

---

### Phase 2 — Module `common`

**Mục tiêu:** Định nghĩa các building block dùng chung cho mọi domain.

- [ ] `BaseId<T>` — generic value object cho ID
- [ ] `DomainEvent` — interface base cho tất cả domain event
- [ ] `DomainException` — base exception
- [ ] `Money`, `Quantity` — value object phổ biến (nếu cần)
- [ ] Enum dùng chung (nếu có)

---

### Phase 3 — Domain Core (mỗi domain)

**Thực hiện lần lượt hoặc song song cho: `user`, `booking`, `ticket`**

#### 3.1 Thiết kế nghiệp vụ trước khi code
- [ ] Xác định Aggregate Root, Entity, Value Object của từng domain
- [ ] Vẽ sơ đồ quan hệ giữa các domain (User tạo Booking, Booking sinh ra Ticket...)
- [ ] Xác định các Domain Event (UserCreated, BookingConfirmed, TicketIssued...)
- [ ] Xác định Input Port (use case nào cần expose)
- [ ] Xác định Output Port (cần query/persist gì, cần publish event gì)

#### 3.2 Code Domain Core
- [ ] Viết Entity / Aggregate Root với business logic bên trong
- [ ] Viết Value Object (immutable, tự validate)
- [ ] Viết Domain Event
- [ ] Viết Domain Service (logic không thuộc về entity nào)
- [ ] Định nghĩa Input Port (interface use case)
- [ ] Định nghĩa Output Port (interface repository, publisher)

#### 3.3 Unit Test cho Domain Core
- [ ] Test mỗi method của Entity (business rule validation)
- [ ] Test Domain Service
- [ ] Không dùng Spring context — thuần JUnit 5 + Mockito
- [ ] Coverage mục tiêu: **≥ 80%** cho domain layer

---

### Phase 4 — Application Layer (mỗi domain)

**Mục tiêu:** Implement use case, orchestrate domain objects.

- [ ] Viết Command / Query DTO
- [ ] Viết `DataMapper` (Command → Entity, Entity → Response DTO) — dùng MapStruct
- [ ] Implement `ApplicationService` (implements Input Port)
  - Validate command
  - Gọi Domain Service
  - Persist qua Output Port (repository)
  - Publish event qua Output Port (publisher)
  - Trả về response DTO
- [ ] Unit test cho từng Application Service method
  - Mock tất cả Output Port
  - Kiểm tra logic orchestration

---

### Phase 5 — Data Access Layer (mỗi domain)

**Mục tiêu:** Implement persistence adapter cho PostgreSQL và Redis.

#### 5.1 PostgreSQL (JPA)
- [ ] Viết JPA Entity (tách biệt hoàn toàn với Domain Entity)
- [ ] Viết Spring Data `JpaRepository`
- [ ] Viết `DataAccessMapper` (JPA Entity ↔ Domain Entity)
- [ ] Viết `RepositoryImpl` — implement Output Port, gọi JPA Repository
- [ ] Viết Flyway/Liquibase migration script cho schema

#### 5.2 Redis (Caching)
- [ ] Cấu hình `RedisTemplate` / `@Cacheable`
- [ ] Viết `CacheAdapter` — implement Output Port hoặc dùng Spring Cache abstraction
- [ ] Định nghĩa cache key strategy, TTL cho từng entity

#### 5.3 Unit Test Data Access
- [ ] Test mapper (JPA Entity ↔ Domain Entity)
- [ ] Test adapter logic (mock JpaRepository)

---

### Phase 6 — Messaging Layer (mỗi domain)

**Mục tiêu:** Tích hợp RabbitMQ cho event-driven communication giữa các domain.

- [ ] Thiết kế Exchange, Queue, Routing Key cho từng domain event
- [ ] Viết `EventPublisher` — implement Output Port, dùng `RabbitTemplate`
- [ ] Viết `EventListener` — `@RabbitListener`, deserialize message, gọi Application Service
- [ ] Viết `MessagingMapper` (Domain Event ↔ Message DTO / JSON)
- [ ] Cấu hình Dead Letter Queue (DLQ) cho error handling
- [ ] Unit test publisher và listener (mock RabbitTemplate)

---

### Phase 7 — REST Container Layer (mỗi domain)

**Mục tiêu:** Expose HTTP API, wire toàn bộ DI.

- [ ] Viết `@RestController` — gọi Input Port (ApplicationService)
- [ ] Validate request với Bean Validation (`@Valid`, `@NotNull`...)
- [ ] Viết `@RestControllerAdvice` — global exception handler
  - Map `DomainException` → HTTP 400/404/409
  - Map các exception khác → HTTP 500
- [ ] Viết `BeanConfig` — `@Configuration` @Bean để wire implementation vào interface
- [ ] Tích hợp SpringDoc OpenAPI (Swagger UI)
- [ ] Unit test Controller (MockMvc, mock ApplicationService)

---

### Phase 8 — Module `app` (Runner)

**Mục tiêu:** Entry point duy nhất của ứng dụng, kết nối tất cả module.

- [ ] Viết `MultiModuleApplication.java` với `@SpringBootApplication`
- [ ] Cấu hình `application.yml` (datasource, redis, rabbitmq, server port)
- [ ] Cấu hình profile: `dev`, `test`, `prod`
- [ ] Cấu hình Security (Spring Security 6 — authentication cơ bản hoặc JWT)
- [ ] Actuator endpoints (health, metrics, info)
- [ ] `build.gradle` của `app` phụ thuộc tất cả `*-container`

---

### Phase 9 — Docker & Infrastructure

**Mục tiêu:** Containerize toàn bộ stack, chạy được bằng 1 lệnh.

#### 9.1 Dockerfile
```dockerfile
# Multi-stage build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 9.2 docker-compose.yml
- [ ] Service `postgres` — PostgreSQL 18, volume persistent, healthcheck
- [ ] Service `redis` — Redis 8.4, volume persistent
- [ ] Service `rabbitmq` — RabbitMQ 4.x với Management UI (port 15672)
- [ ] Service `app` — build từ Dockerfile, depends_on postgres + redis + rabbitmq
- [ ] Service `nginx` — 1.28.1, reverse proxy trỏ vào `app:8080`
- [ ] Network `app-network` dùng chung

#### 9.3 Nginx Config
- [ ] Reverse proxy `/api/*` → `app:8080`
- [ ] Rate limiting cơ bản
- [ ] Gzip compression

#### 9.4 Các lệnh chạy
```bash
# Build image và chạy toàn bộ stack
docker compose up --build -d

# Xem logs
docker compose logs -f app

# Dừng
docker compose down -v
```

---

### Phase 10 — Unit Testing (Tổng kết)

**Mục tiêu:** Đảm bảo mỗi function đều có unit test.

| Layer | Test Tool | Scope |
|-------|-----------|-------|
| Domain Core | JUnit 5, Mockito | Entity method, Domain Service |
| Application | JUnit 5, Mockito | Use case orchestration, mock ports |
| Data Access | JUnit 5, Mockito | Mapper, mock JPA repo |
| Messaging | JUnit 5, Mockito | Publisher, Listener logic |
| REST Controller | JUnit 5, MockMvc | HTTP request/response, mock service |

**Quy tắc viết test:**
- Mỗi method public → ít nhất 1 test happy path + 1 test edge case / exception
- Tên test theo pattern: `methodName_givenCondition_expectedBehavior()`
- Không test framework code (getter/setter, Spring auto-wiring)

---

### Phase 11 — Integration Test với TestContainers

**Mục tiêu:** E2E test cho các API quan trọng với infrastructure thật (không mock DB/Redis/RabbitMQ).

#### 11.1 Setup TestContainers
```java
// Base class cho tất cả integration test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8.4").withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }
}
```

#### 11.2 Các API cần test E2E (ưu tiên)
- [ ] `POST /api/users` — Tạo user mới (test persist DB + publish event)
- [ ] `POST /api/bookings` — Tạo booking (test validate user tồn tại + persist)
- [ ] `GET /api/bookings/{id}` — Lấy booking (test cache Redis hit/miss)
- [ ] `POST /api/tickets/issue` — Phát hành ticket từ booking (test async flow qua RabbitMQ)
- [ ] `GET /api/users/{id}` — Lấy user (test Redis caching)

#### 11.3 TestContainers với Gradle
- [ ] Thêm `testcontainers-bom` vào `libs.versions.toml`
- [ ] Chỉ apply TestContainers dependency ở module `app` (integration test)
- [ ] Tách `src/integrationTest/java` riêng với `src/test/java`

---

## 4. Thứ tự ưu tiên thực hiện

```
Phase 1 (Gradle Setup)
    │
    ▼
Phase 2 (Common Module)
    │
    ▼
Phase 3 (Domain Core — user) ──► Phase 10 (Unit Test domain)
    │
    ▼
Phase 4 (Application — user) ──► Phase 10 (Unit Test app)
    │
    ├──► Phase 5 (DataAccess — user) ──► Phase 10 (Unit Test)
    ├──► Phase 6 (Messaging — user)  ──► Phase 10 (Unit Test)
    └──► Phase 7 (Container — user)  ──► Phase 10 (Unit Test)
    │
    ▼
[Lặp lại Phase 3-7 cho booking, ticket]
    │
    ▼
Phase 8 (App Runner)
    │
    ▼
Phase 9 (Docker & Infra)
    │
    ▼
Phase 11 (E2E TestContainers)
```

---

## 5. Phân tích nghiệp vụ 3 Domain

### Domain: User
| Use Case | Input | Output | Event |
|----------|-------|--------|-------|
| Tạo user | CreateUserCommand | UserResponse | UserCreatedEvent |
| Lấy user | userId | UserResponse | — |
| Cập nhật user | UpdateUserCommand | UserResponse | UserUpdatedEvent |
| Xoá user | userId | — | UserDeletedEvent |

### Domain: Booking
| Use Case | Input | Output | Event |
|----------|-------|--------|-------|
| Tạo booking | CreateBookingCommand (userId, ...) | BookingResponse | BookingCreatedEvent |
| Xác nhận booking | bookingId | BookingResponse | BookingConfirmedEvent |
| Huỷ booking | bookingId | BookingResponse | BookingCancelledEvent |
| Lấy booking | bookingId | BookingResponse | — |

### Domain: Ticket
| Use Case | Input | Output | Event |
|----------|-------|--------|-------|
| Phát hành ticket | BookingConfirmedEvent (async) | — | TicketIssuedEvent |
| Lấy ticket | ticketId | TicketResponse | — |
| Huỷ ticket | ticketId | TicketResponse | TicketCancelledEvent |

**Flow tổng thể:**
```
Client → POST /api/bookings
    → BookingApplicationService.createBooking()
    → BookingDomainService.validateAndInitiate()
    → BookingRepository.save()
    → BookingMessagePublisher.publish(BookingCreatedEvent)
    → [RabbitMQ] → TicketEventListener.onBookingCreated()
    → TicketApplicationService.issueTicket()
    → TicketRepository.save()
```

---

## 6. Dependency Graph giữa các Module

```
common
  ├── user-domain       depends on: common
  ├── user-application  depends on: user-domain
  ├── user-dataaccess   depends on: user-domain
  ├── user-messaging    depends on: user-domain
  └── user-container    depends on: user-application, user-dataaccess, user-messaging

  ├── booking-domain       depends on: common
  ├── booking-application  depends on: booking-domain
  ├── booking-dataaccess   depends on: booking-domain
  ├── booking-messaging    depends on: booking-domain
  └── booking-container    depends on: booking-application, booking-dataaccess, booking-messaging

  ├── ticket-domain       depends on: common
  ├── ticket-application  depends on: ticket-domain
  ├── ticket-dataaccess   depends on: ticket-domain
  ├── ticket-messaging    depends on: ticket-domain
  └── ticket-container    depends on: ticket-application, ticket-dataaccess, ticket-messaging

app  depends on: user-container, booking-container, ticket-container
```

**Quy tắc bắt buộc:**
- Domain layer **KHÔNG BAO GIỜ** import Spring, JPA, Redis, RabbitMQ
- Domain layer chỉ dùng Java thuần + `common`
- Spring annotation chỉ xuất hiện từ Application layer trở ra

---

## 7. Checklist trước khi coi là Done

- [ ] `./gradlew build` pass toàn bộ (bao gồm unit test)
- [ ] `docker compose up --build` chạy được, app healthy
- [ ] Swagger UI accessible tại `http://localhost/swagger-ui.html`
- [ ] RabbitMQ Management UI tại `http://localhost:15672`
- [ ] Tất cả API trong Phase 11 có integration test pass
- [ ] Không có circular dependency giữa các module
- [ ] Domain layer không có Spring import
- [ ] Code coverage ≥ 70% toàn project (đo bằng JaCoCo)
