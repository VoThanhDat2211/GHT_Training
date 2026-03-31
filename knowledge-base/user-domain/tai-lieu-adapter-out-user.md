# Tai lieu adapter out cho user module

## Muc tieu

Tai lieu nay mo ta phan `adapter/out` vua duoc bo sung cho module `user` de hien thuc cac output port trong tang `application`.

Pham vi:
- persistence adapter cho `UserRepository`
- cache adapter cho `UserQueryRepository`
- message publisher theo huong outbox cho `UserMessagePublisher`

## Tong quan kien truc

Trong module `user`, output port van nam o:
- `com.multimodule.user.application.port.output.UserRepository`
- `com.multimodule.user.application.port.output.UserQueryRepository`
- `com.multimodule.user.application.port.output.UserMessagePublisher`

Phan implement nam o `adapter/out`:
- `adapter/out/persistence`
- `adapter/out/cache`
- `adapter/out/message`

Luong xu ly hien tai:

1. Command service goi `UserRepository`
2. `UserPersistenceAdapter` ghi vao PostgreSQL qua Spring Data JPA
3. Sau khi ghi thanh cong, cache user trong Redis bi xoa
4. Query service goi `UserQueryRepository`
5. `UserQueryCacheAdapter` doc Redis truoc, neu miss moi query PostgreSQL
6. Khi tao user, `UserOutboxMessagePublisher` ghi su kien vao bang `outbox_events`

## Persistence adapter

File chinh:
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/UserPersistenceAdapter.java`
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/entity/UserJpaEntity.java`
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/repository/UserJpaRepository.java`
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/mapper/UserPersistenceMapper.java`

Trach nhiem:
- map domain `User` sang `UserJpaEntity`
- luu va doc du lieu tu bang `users`
- phuc vu cac check unique cho `email` va `username`
- xoa cache Redis sau moi lan `save`

Luu y:
- `findById` trong `UserPersistenceAdapter` van ton tai vi command service can doc user de update, block, activate, delete
- adapter nay khong chiu trach nhiem cache read-through, chi lo write va cache invalidation

## Redis read-through cho query

File chinh:
- `user/src/main/java/com/multimodule/user/adapter/out/cache/UserQueryCacheAdapter.java`

Muc tieu:
- toi uu truy van `get user by id`
- giu cho `GetUserByIdService` chi phu thuoc `UserQueryRepository`, khong biet toi Redis hay PostgreSQL

Co che:

1. Tao key theo format `user:{userId}`
2. Doc `StringRedisTemplate.opsForValue().get(key)`
3. Neu co du lieu trong cache:
   - deserialize JSON thanh `User`
   - tra ket qua ngay
4. Neu deserialize loi:
   - ghi log warn
   - xoa cache key loi
   - fallback xuong PostgreSQL
5. Neu cache miss:
   - query `UserJpaRepository.findByIdAndDeletedFalse(...)`
   - map sang domain `User`
   - ghi lai cache voi TTL 10 phut

Thiet ke nay la read-through o muc adapter, khong day logic cache vao service.

## Cache invalidation

`UserPersistenceAdapter` xoa key Redis sau khi `save` thanh cong:

- create user
- update user
- block user
- activate user
- soft delete user

Ly do:
- tranh stale data khi query ngay sau command
- de don gian hon so voi cap nhat partial cache
- phu hop voi luong hien tai vi query chinh moi chi la `findById`

## Message publisher theo outbox

File chinh:
- `user/src/main/java/com/multimodule/user/adapter/out/message/UserOutboxMessagePublisher.java`
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/entity/OutboxEventJpaEntity.java`
- `user/src/main/java/com/multimodule/user/adapter/out/persistence/repository/OutboxEventJpaRepository.java`

Muc tieu:
- chua publish truc tiep len RabbitMQ trong transaction tao user
- luu event ben vung vao PostgreSQL truoc
- phu hop huong thiet ke da mo ta trong `knowledge-base/design-bussiness.md`

Hanh vi hien tai:
- khi `CreateUserService` tao user thanh cong, no goi `UserMessagePublisher`
- `UserOutboxMessagePublisher` tao ban ghi moi trong `outbox_events`
- status mac dinh la `PENDING`
- payload duoc serialize JSON bang Jackson

Phan nay moi dung o muc ghi outbox. Chua co worker/doc publisher de:
- doc outbox pending
- publish len RabbitMQ
- cap nhat status thanh `PUBLISHED`

## Dependency bo sung

Trong `user/build.gradle` da bo sung:
- `jackson-databind`
- `jackson-datatype-jsr310`

Ly do:
- serialize payload outbox
- serialize/deserialize `LocalDateTime` cho Redis cache document

## Test da bo sung

File test:
- `user/src/test/java/com/multimodule/user/adapter/out/cache/UserQueryCacheAdapterTest.java`

Case da cover:
- cache hit: doc tu Redis, khong query PostgreSQL
- cache miss: query PostgreSQL, sau do set lai Redis voi TTL 10 phut

Ngoai ra test cu cua application service van pass, cho thay wiring moi khong pha vo logic command/query dang co.

## Lenh verify

Lenh da dung de verify:

```powershell
.\gradlew.bat :user:test
```

Ket qua:
- `BUILD SUCCESSFUL`

## Gioi han hien tai

- chua co integration test thuc te voi PostgreSQL + Redis
- chua co outbox processor de day su kien len RabbitMQ
- cache hien tai moi phuc vu `findById`
- chua co metrics cho cache hit/miss

## Huong mo rong tiep theo

1. Them integration test cho `GetUserByIdService` voi Redis va PostgreSQL that
2. Them outbox publisher scheduler/job de day event sang RabbitMQ
3. Them cache policy ro hon cho query theo `email` hoac `username` neu sau nay can
4. Can nhac tach cache key helper va TTL config ra `application.yml`
