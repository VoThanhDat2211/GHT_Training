# Kết Nối Services Khi Chạy Docker Và Local

## Cơ chế

`application.yml` dùng placeholder kiểu:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:postgres}
```

Spring Boot sẽ:

- dùng biến môi trường nếu có
- nếu không có thì dùng giá trị mặc định sau dấu `:`

## App chạy trong Docker

`docker-compose.yml` truyền env cho service `app`:

```yaml
environment:
  DB_HOST: postgres
  REDIS_HOST: redis
  RABBITMQ_HOST: rabbitmq
```

`postgres`, `redis`, `rabbitmq` là tên service trong Docker network, nên app gọi trực tiếp theo hostname đó.

## App chạy local

Nếu chạy bằng IntelliJ hoặc:

```powershell
.\gradlew.bat :app:bootRun
```

thì app không ở trong Docker network, nên nó dùng default:

- `localhost:5432` neu khong truyen bien moi truong
- `localhost:6379`
- `localhost:5672`

Luu y voi cau hinh hien tai cua `docker-compose.yml`:

- Postgres dang publish `5433:5432`
- nen app local phai dung `localhost:5433` neu muon vao dung Postgres container
- vi vay can truyen them `DB_PORT=5433` khi chay local, hoac doi default trong `application.yml`

## Vì sao localhost vẫn vào được container

Do `docker-compose.yml` publish port ra host:

```yaml
ports:
  - "5433:5432"   # postgres
  - "6379:6379"
  - "5672:5672"
```

Nên app local gọi `localhost:port` là đi vào đúng container.

Cu the voi cau hinh hien tai:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`

## Kết luận

- App trong Docker: dùng tên service Docker
- App local: dùng `localhost`
- Rieng PostgreSQL local hien tai phai dung `localhost:5433`
- Cả hai cùng chạy được vì:
  - Spring Boot resolve env/default từ `application.yml`
  - Docker Compose inject env cho container `app`
  - Docker Compose publish port ra máy host
