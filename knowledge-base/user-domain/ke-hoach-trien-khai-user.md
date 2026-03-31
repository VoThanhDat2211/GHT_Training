# Kế Hoạch Triển Khai User Module

## 1. Mục tiêu

Triển khai hoàn chỉnh module `user` theo mô tả trong `knowledge-base/design-bussiness.md`, bám theo kiến trúc hexagonal hiện có và bảo đảm model dữ liệu, API, persistence, cache, và test khớp với business spec.

## 2. Hiện trạng

Module `user` đã có khung tương đối đầy đủ:

- `domain`
- `application`
- `adapter/in/web`
- `adapter/out/persistence`
- `adapter/out/messaging`

Tuy nhiên, implementation hiện tại còn lệch với tài liệu nghiệp vụ:

- entity `User` chưa có đủ trường `username`, `fullName`, `phoneNumber`, `updatedAt`, `deletedAt`, `isDeleted`
- `delete` đang là hard delete, chưa phải soft delete
- `UserStatus` chưa phản ánh đầy đủ trạng thái trong business spec
- API mới chỉ có create, get by id, delete
- chưa có update, block, activate
- publish event đang đi thẳng RabbitMQ, chưa theo hướng outbox trong tài liệu tổng thể

## 3. Phạm vi chức năng cần triển khai

Theo tài liệu nghiệp vụ, module `user` cần có:

- Tạo user
- Lấy user theo ID
- Cập nhật user
- Xóa mềm user
- Khóa user
- Mở khóa user

API mục tiêu:

- `POST /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `PATCH /api/users/{id}/block`
- `PATCH /api/users/{id}/activate`

## 4. Kế hoạch triển khai theo pha

### Pha 1: Chuẩn hóa domain model và persistence

Mục tiêu của pha này là đưa `user` về đúng business model trước khi mở rộng use case.

Việc cần làm:

1. Refactor aggregate `User` để có đầy đủ thuộc tính:
   - `id`
   - `username`
   - `email`
   - `fullName`
   - `phoneNumber`
   - `status`
   - `isDeleted`
   - `createdAt`
   - `updatedAt`
   - `deletedAt`
2. Chuẩn hóa `UserStatus` thành:
   - `ACTIVE`
   - `INACTIVE`
   - `BLOCKED`
3. Bổ sung business behavior trong domain:
   - `create`
   - `updateProfile`
   - `softDelete`
   - `block`
   - `activate`
4. Chuẩn hóa validation domain:
   - username không rỗng
   - email hợp lệ
   - full name không rỗng
   - không thao tác trên user đã bị xóa
5. Cập nhật `UserJpaEntity`, mapper, repository để map đúng schema `users`.
6. Chuyển `delete` từ hard delete sang soft delete.
7. Đảm bảo các truy vấn chỉ lấy user chưa bị xóa, trừ khi có nhu cầu riêng cho admin.

### Pha 2: Thiết kế schema và migration

Mục tiêu là đồng bộ database với tài liệu nghiệp vụ.

Việc cần làm:

1. Thêm Flyway vào `app` nếu chưa có.
2. Tạo migration trong `app/src/main/resources/db/migration/`:
   - `V1__create_users_table.sql`
   - `V2__create_users_indexes.sql`
3. Schema `users` nên bám theo tài liệu:
   - `id uuid primary key`
   - `username varchar(50) unique not null`
   - `email varchar(100) unique not null`
   - `full_name varchar(100) not null`
   - `phone_number varchar(20)`
   - `status varchar(20) not null`
   - `is_deleted boolean not null default false`
   - `created_at timestamptz not null`
   - `updated_at timestamptz not null`
   - `deleted_at timestamptz`
4. Tạo index cho:
   - `email`
   - `username`
   - `status`

### Pha 3: Hoàn thiện application layer và API

Mục tiêu là triển khai đủ các use case trong tài liệu.

Việc cần làm:

1. Bổ sung input ports:
   - `UpdateUserUseCase`
   - `BlockUserUseCase`
   - `ActivateUserUseCase`
2. Bổ sung application services tương ứng.
3. Mở rộng output ports nếu cần:
   - kiểm tra trùng `username`
   - truy vấn user active/non-deleted
4. Thêm DTO:
   - `UpdateUserCommand`
   - request/response cho update, block, activate
5. Cập nhật `UserController` để expose đầy đủ API mục tiêu.
6. Chuẩn hóa lỗi nghiệp vụ và HTTP mapping:
   - `404` khi không tìm thấy user
   - `409` khi trùng `email` hoặc `username`
   - `400` khi request không hợp lệ

### Pha 4: Cache và đồng bộ dữ liệu đọc

Mục tiêu là bám theo chiến lược Redis trong tài liệu.

Việc cần làm:

1. Giữ pattern cache key: `user:{id}`.
2. Cache cho `getUserById`.
3. Evict cache khi:
   - update user
   - delete user
   - block user
   - activate user
4. Kiểm tra lại khả năng serialize object vào Redis, tránh lưu trực tiếp domain object nếu dễ gây lỗi versioning.

### Pha 5: Event và hướng outbox

Mục tiêu là không để phần event lệch hướng với thiết kế tổng thể.

Việc cần làm:

1. Giữ `UserCreatedEvent` nếu cần cho use case hiện tại.
2. Không mở rộng messaging tùy tiện theo kiểu publish trực tiếp thêm nhiều event mới.
3. Tách backlog riêng cho outbox pattern vì đây là hạ tầng dùng chung cho nhiều module.
4. Nếu triển khai event ở user ngay, cần chốt rõ:
   - event nào là bắt buộc
   - payload nào là contract chính thức
   - thời điểm publish theo transaction boundary nào

### Pha 6: Kiểm thử

Mục tiêu là khóa hành vi trước khi triển khai booking và ticket phụ thuộc vào `user`.

Việc cần làm:

1. Domain tests:
   - tạo user hợp lệ
   - chặn dữ liệu không hợp lệ
   - block user
   - activate user
   - soft delete user
2. Application service tests:
   - create
   - update
   - get by id
   - block
   - activate
   - delete
3. Controller tests:
   - status code
   - request validation
   - response payload
4. Persistence/integration tests:
   - mapping JPA
   - query filter với `is_deleted = false`
   - cache hit/miss
5. ArchUnit tests:
   - bảo vệ boundary giữa domain, application, adapter

## 5. Thứ tự ưu tiên đề xuất

Nên triển khai theo thứ tự sau:

1. Refactor domain model
2. Đồng bộ JPA entity, mapper, repository
3. Thêm migration và schema chuẩn
4. Implement update, block, activate
5. Đổi delete sang soft delete
6. Hoàn thiện cache eviction
7. Bổ sung test toàn bộ luồng user

## 6. Rủi ro và lưu ý kỹ thuật

- Không nên phát triển booking/ticket dựa trên model `user` hiện tại vì contract còn thiếu trường.
- Nếu chưa làm outbox ngay, cần ghi rõ đây là giới hạn tạm thời để tránh hiểu nhầm kiến trúc cuối cùng.
- Redis hiện đang cache trực tiếp object domain; nên kiểm tra sớm chiến lược serialization.
- Khi thêm `username` và `email` unique, cần chuẩn hóa rule update để không tự xung đột với chính bản ghi hiện tại.

## 7. Kết quả mong đợi sau khi hoàn thành

Sau khi hoàn thành kế hoạch này, module `user` sẽ:

- khớp với business spec trong tài liệu thiết kế
- có đầy đủ API quản lý vòng đời user
- dùng đúng soft delete
- có nền đủ ổn định để module `booking` phụ thuộc vào
- có test bảo vệ hành vi trước khi mở rộng sang booking và ticket
