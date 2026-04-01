# Kế Hoạch Triển Khai Booking Module

## 1. Mục tiêu

Triển khai hoàn chỉnh module `booking` theo mô tả trong `knowledge-base/design-bussiness.md`, bám theo kiến trúc hexagonal hiện có và bảo đảm phần booking trở thành trung tâm của luồng nghiệp vụ:

- user tạo booking
- booking được quản lý vòng đời từ `PENDING` đến `CONFIRMED` hoặc `CANCELLED` hoặc `EXPIRED`
- khi booking đổi trạng thái quan trọng, hệ thống ghi event vào `outbox_events`
- event được publish qua RabbitMQ để `ticket` xử lý bất đồng bộ

## 2. Vai trò của booking trong nghiệp vụ

`booking` là module trung tâm nối giữa `user` và `ticket`.

Trách nhiệm chính:

- tiếp nhận yêu cầu đặt chỗ từ user
- lưu đầy đủ thông tin booking và booking items
- quản lý trạng thái nghiệp vụ của booking
- kiểm soát các mốc thời gian như `bookedAt`, `confirmedAt`, `cancelledAt`, `expiresAt`
- phát sinh domain event khi booking được xác nhận hoặc bị hủy

Booking là điểm bắt đầu cho các luồng downstream:

- `BookingConfirmed` -> ticket issue
- `BookingCancelled` -> ticket cancel

## 3. Hiện trạng và giả định triển khai

Theo tài liệu nghiệp vụ, module `booking` cần làm việc trên các bảng:

- `bookings`
- `booking_items`
- `outbox_events`

Các phụ thuộc nghiệp vụ chính:

- `user` phải tồn tại và hợp lệ để tạo booking
- `ticket` không được tự phát hành nếu chưa có `BookingConfirmed`

Giả định kỹ thuật:

- PostgreSQL là source of truth
- Redis chỉ dùng cho tối ưu đọc
- RabbitMQ không publish trực tiếp từ transaction nghiệp vụ, mà đi qua outbox

## 4. Phạm vi chức năng cần triển khai

Theo business spec, module `booking` cần có các chức năng sau:

- Tạo booking
- Xem booking theo ID
- Xem danh sách booking của user
- Xác nhận booking
- Hủy booking
- Xử lý booking hết hạn

API mục tiêu:

- `POST /api/bookings`
- `GET /api/bookings/{id}`
- `GET /api/users/{userId}/bookings`
- `PATCH /api/bookings/{id}/confirm`
- `PATCH /api/bookings/{id}/cancel`

Ngoài API đồng bộ, cần thêm:

- job xử lý booking hết hạn
- outbox publisher cho các event của booking

## 5. Mô tả nghiệp vụ từng chức năng

### 5.1. Tạo booking

Mục tiêu:

- tạo một booking mới cho một user
- lưu danh sách `booking_items`
- khởi tạo trạng thái ban đầu là `PENDING`

Input nghiệp vụ tối thiểu:

- `userId`
- danh sách item
- `totalAmount`
- `currency`
- `expiresAt`

Luồng xử lý:

1. kiểm tra `userId` tồn tại
2. kiểm tra user có đang ở trạng thái cho phép đặt booking hay không
3. kiểm tra danh sách item hợp lệ, không rỗng
4. tính hoặc xác thực `totalAmount` từ các item
5. sinh `bookingCode`
6. tạo aggregate `Booking` với trạng thái `PENDING`
7. lưu `bookings` và `booking_items`

Kết quả mong đợi:

- booking được tạo thành công
- chưa phát sinh RabbitMQ event ở bước này nếu chỉ bám theo business spec hiện tại

Lưu ý:

- không nên publish `BookingCreated` nếu hệ thống chưa thật sự cần contract này
- `expiresAt` phải rõ ràng để job hết hạn có thể chạy đúng

### 5.2. Xem booking theo ID

Mục tiêu:

- trả về chi tiết booking và các booking item

Luồng xử lý:

1. tìm booking theo `id`
2. kiểm tra booking tồn tại
3. load `booking_items`
4. trả về response chi tiết

Kết quả mong đợi:

- client đọc được booking hiện tại cùng trạng thái và item

### 5.3. Xem danh sách booking của user

Mục tiêu:

- trả về danh sách booking theo `userId`

Luồng xử lý:

1. kiểm tra user tồn tại nếu cần chặt chẽ nghiệp vụ
2. truy vấn danh sách booking theo `userId`
3. hỗ trợ sắp xếp theo `createdAt` hoặc `bookedAt` giảm dần

Kết quả mong đợi:

- client xem được lịch sử booking của user

Lưu ý:

- chức năng này thường nên có phân trang từ đầu

### 5.4. Xác nhận booking

Mục tiêu:

- chuyển booking từ `PENDING` sang `CONFIRMED`
- đánh dấu thời điểm xác nhận
- phát sinh event cho module `ticket`

Điều kiện nghiệp vụ:

- booking phải đang ở trạng thái `PENDING`
- booking chưa hết hạn
- booking chưa bị hủy

Luồng xử lý:

1. load booking theo `id`
2. kiểm tra trạng thái hiện tại
3. kiểm tra `expiresAt > now`
4. chuyển trạng thái sang `CONFIRMED`
5. set `confirmedAt`
6. cập nhật booking
7. trong cùng transaction, ghi `BookingConfirmed` vào `outbox_events`
8. evict cache liên quan

Kết quả mong đợi:

- booking được xác nhận thành công
- event sẵn sàng để outbox publisher đẩy sang RabbitMQ

Đây là chức năng quan trọng nhất của module `booking` vì nó kích hoạt luồng downstream cho `ticket`.

### 5.5. Hủy booking

Mục tiêu:

- chuyển booking sang `CANCELLED`
- lưu thời điểm hủy và lý do hủy
- thông báo cho downstream nếu ticket đã được phát hành hoặc đang chờ xử lý

Điều kiện nghiệp vụ:

- booking không được ở trạng thái `CANCELLED`
- booking không được ở trạng thái `EXPIRED`
- rule có cho hủy `CONFIRMED` hay không phải chốt rõ

Khuyến nghị trước mắt:

- cho phép hủy `PENDING`
- nếu cho hủy `CONFIRMED`, phải thống nhất ticket cũng phải xử lý cancel theo event

Luồng xử lý:

1. load booking
2. kiểm tra trạng thái hiện tại
3. chuyển sang `CANCELLED`
4. set `cancelledAt`, `cancelReason`
5. cập nhật booking
6. trong cùng transaction, ghi `BookingCancelled` vào `outbox_events`
7. evict cache liên quan

Kết quả mong đợi:

- booking bị hủy đúng trạng thái
- ticket module có thể consume event để hủy ticket liên quan

### 5.6. Xử lý booking hết hạn

Mục tiêu:

- tự động chuyển các booking `PENDING` đã quá hạn sang `EXPIRED`

Điều kiện nghiệp vụ:

- chỉ áp dụng cho booking có trạng thái `PENDING`
- `expiresAt < now`

Luồng xử lý:

1. job định kỳ quét các booking `PENDING` quá hạn
2. cập nhật trạng thái sang `EXPIRED`
3. set `updatedAt`
4. evict cache nếu booking đó đã từng được cache

Kết quả mong đợi:

- không còn booking pending treo quá hạn

Lưu ý:

- business spec hiện tại chưa yêu cầu publish event `BookingExpired`
- chưa cần RabbitMQ cho luồng này nếu downstream không dùng

## 6. Trạng thái nghiệp vụ và rule chuyển trạng thái

Trạng thái của `booking`:

- `PENDING`
- `CONFIRMED`
- `CANCELLED`
- `EXPIRED`

Rule chuyển trạng thái đề xuất:

- `PENDING` -> `CONFIRMED`
- `PENDING` -> `CANCELLED`
- `PENDING` -> `EXPIRED`
- `CONFIRMED` -> `CANCELLED` nếu business cho phép hủy sau xác nhận

Không hợp lệ:

- `CANCELLED` -> bất kỳ trạng thái nào khác
- `EXPIRED` -> `CONFIRMED`
- `CONFIRMED` -> `PENDING`

## 7. Kế hoạch triển khai theo pha

### Pha 1: Chuẩn hóa domain model

Mục tiêu:

- dựng đúng aggregate và business rule của booking

Việc cần làm:

1. tạo aggregate `Booking`
2. tạo entity hoặc value object `BookingItem`
3. tạo enum `BookingStatus`
4. thêm behavior domain:
   - `create`
   - `confirm`
   - `cancel`
   - `expire`
5. validate domain:
   - userId không rỗng
   - items không rỗng
   - amount hợp lệ
   - không confirm booking đã hết hạn
   - không thao tác sai transition trạng thái

### Pha 2: Persistence và migration

Mục tiêu:

- đồng bộ database với booking model

Việc cần làm:

1. triển khai JPA entity cho `bookings`
2. triển khai JPA entity cho `booking_items`
3. tạo repository và mapper persistence
4. bảo đảm mapping đúng quan hệ `Booking 1 - n BookingItem`
5. dùng migration cho:
   - bảng `bookings`
   - bảng `booking_items`
   - index theo `user_id`, `status`, `booking_code`, `expires_at`

### Pha 3: Application layer và API

Mục tiêu:

- expose đầy đủ use case booking

Việc cần làm:

1. tạo input ports:
   - `CreateBookingUseCase`
   - `GetBookingByIdUseCase`
   - `GetBookingsByUserUseCase`
   - `ConfirmBookingUseCase`
   - `CancelBookingUseCase`
2. tạo command và response DTO
3. triển khai application services tương ứng
4. tạo controller cho các API mục tiêu
5. chuẩn hóa lỗi:
   - `404` khi không tìm thấy booking
   - `400` khi request không hợp lệ
   - `409` khi transition trạng thái không hợp lệ

### Pha 4: Cache Redis

Mục tiêu:

- tối ưu read path cho booking detail

Phần cần dùng cache:

- `GET /api/bookings/{id}`

Cache đề xuất:

- cache name: `bookingById`
- cache key pattern: `booking:{id}`

Phần không nên cache ở giai đoạn đầu:

- danh sách booking theo user
- danh sách kết quả tìm kiếm hoặc lọc trạng thái

Lý do:

- danh sách thay đổi thường xuyên và dễ phát sinh bài toán invalidation phức tạp
- lợi ích cache theo ID rõ ràng hơn và an toàn hơn

Cache eviction bắt buộc khi:

- tạo booking nếu có chiến lược đọc lại ngay theo ID
- xác nhận booking
- hủy booking
- job expire booking
- cập nhật booking nếu sau này có use case chỉnh sửa

Lưu ý kỹ thuật:

- chỉ cache response model hoặc read model, không nên cache trực tiếp domain aggregate
- cần cân nhắc TTL ngắn hoặc evict thuần theo write path

### Pha 5: RabbitMQ và outbox

Mục tiêu:

- bảo đảm event booking không bị mất và ticket nhận được tín hiệu đúng thời điểm

Phần bắt buộc dùng outbox + RabbitMQ:

- `confirm booking` -> ghi `BookingConfirmed`
- `cancel booking` -> ghi `BookingCancelled`

Luồng chuẩn:

1. transaction nghiệp vụ cập nhật trạng thái booking
2. trong cùng transaction, ghi bản ghi vào `outbox_events`
3. publisher nền đọc event `PENDING`
4. publish lên RabbitMQ
5. nếu thành công, cập nhật outbox thành `PUBLISHED`
6. nếu lỗi, đánh dấu `FAILED` hoặc retry theo policy

Event cần có:

- `BookingConfirmed`
- `BookingCancelled`

Payload tối thiểu của `BookingConfirmed`:

- `eventType`
- `bookingId`
- `userId`
- `totalAmount`
- `currency`
- `items`
- `confirmedAt`

Payload tối thiểu của `BookingCancelled`:

- `eventType`
- `bookingId`
- `userId`
- `cancelReason`
- `cancelledAt`

Phần chưa cần RabbitMQ ở giai đoạn đầu:

- tạo booking
- đọc booking
- lấy danh sách booking
- expire booking nếu downstream chưa có nhu cầu consume

### Pha 6: Job xử lý hết hạn

Mục tiêu:

- tự động dọn các booking `PENDING` quá hạn

Việc cần làm:

1. tạo scheduler hoặc background job
2. truy vấn booking `PENDING` có `expiresAt < now`
3. cập nhật sang `EXPIRED`
4. evict cache `bookingById`
5. log và metric số lượng booking hết hạn

### Pha 7: Kiểm thử

Mục tiêu:

- khóa hành vi nghiệp vụ trước khi triển khai tiếp `ticket`

Việc cần làm:

1. domain tests:
   - tạo booking hợp lệ
   - confirm hợp lệ
   - chặn confirm booking hết hạn
   - cancel hợp lệ
   - expire booking pending
2. application tests:
   - create
   - get by id
   - get list by user
   - confirm
   - cancel
   - expire job
3. persistence tests:
   - mapping bookings và booking_items
   - query theo userId
   - query booking pending quá hạn
4. cache tests:
   - cache hit khi get by id
   - evict khi confirm và cancel
5. outbox/messaging tests:
   - ghi outbox trong cùng transaction
   - publisher đổi trạng thái `PENDING` -> `PUBLISHED`

## 8. Tóm tắt phần nào cần cache, RabbitMQ, outbox

### Phần cần cache

- `GET /api/bookings/{id}`

### Phần nên cân nhắc cache sau

- `GET /api/users/{userId}/bookings`

### Phần bắt buộc ghi outbox

- xác nhận booking
- hủy booking

### Phần cần publish RabbitMQ

- `BookingConfirmed`
- `BookingCancelled`

### Phần chưa cần RabbitMQ

- tạo booking
- lấy booking theo ID
- lấy danh sách booking theo user
- expire booking

## 9. Thứ tự ưu tiên đề xuất

Nên triển khai theo thứ tự sau:

1. aggregate `Booking`, `BookingItem`, `BookingStatus`
2. persistence cho `bookings` và `booking_items`
3. API create / get by id / get by user
4. confirm booking + outbox `BookingConfirmed`
5. cancel booking + outbox `BookingCancelled`
6. cache cho `getBookingById`
7. job expire booking
8. test đầy đủ

## 10. Kết quả mong đợi sau khi hoàn thành

Sau khi hoàn thành kế hoạch này, module `booking` sẽ:

- đáp ứng đầy đủ nghiệp vụ cốt lõi của phần đặt chỗ
- trở thành trung tâm orchestration cho luồng `user -> booking -> ticket`
- có cache đúng chỗ cho luồng đọc theo ID
- có outbox và RabbitMQ cho các thay đổi trạng thái quan trọng
- tạo nền ổn định để module `ticket` triển khai theo event `BookingConfirmed` và `BookingCancelled`
