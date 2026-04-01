# Quy Tắc Tạo Object Theo Từng Layer

## Vấn đề thường gặp

Trong kiến trúc chia layer, cùng một luồng xử lý có thể xuất hiện nhiều object khác nhau như:

- `Request DTO`
- `Command`
- `Domain Entity`
- `JpaEntity`
- `Response DTO`
- `RestResponse`

Điều này dễ gây rối nếu chỉ nhìn vào việc "cùng là dữ liệu user nhưng sao lại có nhiều class".

## Nguyên tắc dễ nhớ nhất

**Object nào làm parameter hoặc return type cho tầng nào thì nên thuộc tầng đó.**

Có thể hiểu ngắn gọn như sau:

- parameter của controller -> dùng object của web layer
- parameter của use case -> dùng object của application layer
- object xử lý business -> dùng object của domain layer
- object dùng để lưu DB -> dùng object của persistence layer
- object trả về HTTP -> dùng object của web layer

## Phân loại từng loại object

### 1. Request DTO

Ví dụ:

- `CreateUserRequest`
- `UpdateUserRequest`

Mục đích:

- nhận dữ liệu từ HTTP request
- gắn validation cho API
- phản ánh contract của REST API

Quy tắc:

- chỉ dùng ở `controller` hoặc `web adapter`
- không truyền sâu xuống domain

### 2. Command

Ví dụ:

- `CreateUserCommand`
- `UpdateUserCommand`

Mục đích:

- làm input cho use case
- mô tả ý định nghiệp vụ

Quy tắc:

- controller map `Request DTO` sang `Command`
- application service nhận `Command`

### 3. Domain Entity

Ví dụ:

- `User`

Mục đích:

- giữ state nghiệp vụ
- chứa business rule
- đảm bảo invariant của domain

Ví dụ business logic:

- `block()`
- `activate()`
- `softDelete()`
- `updateProfile()`

Quy tắc:

- không chứa annotation web
- không chứa annotation JPA
- không phụ thuộc framework nếu không cần

### 4. Persistence Entity

Ví dụ:

- `UserJpaEntity`

Mục đích:

- map với bảng database
- làm việc với JPA/Hibernate

Quy tắc:

- chỉ dùng ở adapter persistence
- không chứa business rule

### 5. Response DTO

Ví dụ:

- `UserResponse`

Mục đích:

- dữ liệu application layer trả ra cho caller

Quy tắc:

- use case trả `Response`
- không trả trực tiếp domain entity nếu không cần

### 6. Rest Response

Ví dụ:

- `UserRestResponse`

Mục đích:

- dữ liệu trả ra HTTP response

Quy tắc:

- controller trả `RestResponse`
- không trả thẳng `JpaEntity`

## Cách nhớ theo luồng

Luồng đi vào:

`HTTP JSON -> Request DTO -> Command -> Domain Entity -> JpaEntity -> DB`

Luồng đi ra:

`DB -> JpaEntity -> Domain Entity -> Response DTO -> RestResponse -> HTTP JSON`

## Cách hiểu đơn giản

Đừng nghĩ là "có bao nhiêu dữ liệu thì phải tạo bấy nhiêu class".

Hãy nghĩ là:

- ai dùng object này
- object này sinh ra để phục vụ ranh giới nào

Ví dụ:

- nhận JSON từ Postman -> `Request DTO`
- gọi use case -> `Command`
- xử lý nghiệp vụ -> `Domain Entity`
- lưu database -> `JpaEntity`
- trả ra API -> `RestResponse`

## Quy tắc thực dụng để đỡ rối

- làm param cho controller -> dùng `Request DTO`
- làm param cho use case -> dùng `Command`
- làm business object -> dùng `Domain Entity`
- làm object persistence -> dùng `JpaEntity`
- làm output của use case -> dùng `Response DTO`
- làm output HTTP -> dùng `RestResponse`

## Khi nào có thể gộp bớt

Nếu hai object hoàn toàn giống nhau và hệ thống còn nhỏ, có thể tạm dùng chung.

Nhưng chỉ nên gộp khi hiểu rõ mình đang gộp qua ranh giới nào. Nếu gộp bừa thì sẽ dẫn tới:

- web layer biết quá nhiều về domain
- domain bị phụ thuộc persistence
- API contract bị dính với DB schema

## Kết luận

Lý do có nhiều object không phải để "làm màu kiến trúc", mà để:

- tách trách nhiệm
- giữ boundary rõ ràng
- giảm coupling giữa web, application, domain, persistence
- dễ thay đổi từng tầng mà không làm bẩn toàn bộ hệ thống

Một câu nhớ ngắn:

**Đưa đúng loại object cho đúng tầng sử dụng nó.**
