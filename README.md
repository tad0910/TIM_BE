# ALM-Team-Tim-BE

## Đăng ký
POST: http://localhost:8081/auth/register
{
"username": "updateduser5",
"password": "123",
"email": "abc5@gmail.com"
}

## Đăng nhập
POST :http://localhost:8081/auth/login
{
"usernameOrEmail": "updateduser5",
"password": "123"
}

## Lấy thông tin người dùng (tất cả)
GET: http://localhost:8081/users

## Lấy thông tin người dùng theo id
GET: http://localhost:8081/users/{id}

<!-- GET: http://localhost:8081/users/{email} -->

## Cập nhật thông tin người dùng theo id
PUT:http://localhost:8081/users/{id}
{
"username": "user5",
"password": "123",
"email": "abc6@gmail.com",
"phoneNumber": 123456789,
"profileImage": null,
"role": null,
"createdAt": "2025-09-29T17:08:15",
"passwordChangedAt": null
}

## Lấy profile của người dung theo id
GET http://localhost:8081/profile/users/{id}


# QUÊN MK VÀ RESET MK

## NGười dùng quên mk 
POST http://localhost:8081/auth/forgot-password

{
"email": "lehbac05@gmail.com",
}

## NGười dùng xác nhận mã otp
POST http://localhost:8081/auth/verify-otp

{
"email": "lehbac05@gmail.com",
"otp": "654713"
}

## NGười dùng reset mk
POST http://localhost:8081/auth//reset-password

{
"email": "lehbac05@gmail.com",
"reset_token": "HgYvhHNDAm1f9nawYxKYrontS2_VbH7fMuZS4ey48go",
"newPassword": "123456"
}


# UPLOAD ẢNH
## Người dùng up ảnh

1. Upload image (POST /api/users/{userId}/image)

Method: POST
URL: http://localhost:8081/api/users/1/image (thay 1 bằng userId hợp lệ).
Headers:

Content-Type: multipart/form-data

Body:

Chọn form-data.
Key: file, Type: File, Value: Chọn file ảnh (ví dụ: image.jpg).

Kỳ vọng:

Status: 200 OK
Response: "Image uploaded successfully: <uniqueFilename>"

Lưu ý: Nếu user không tồn tại, file sẽ bị xóa và trả về 404. Image chỉ được lưu vào database, không tự động cập nhật profile image.

## NGười dùng lấy ảnh
2. Lấy Image (GET /api/users/{userId}/image)

Method: GET
URL: http://localhost:8081/api/users/1/image
Headers: Không cần (trừ auth).
Kỳ vọng:

Status: 200 OK
Response: "/uploads/<uniqueFilename>"
Status: 404 nếu không có image.

## Người dùng lấy tất cả ảnh
3. Lấy Danh sách Ảnh (GET /api/users/{userId}/images) - COMMENTED OUT

Method: GET
URL: http://localhost:8081/api/users/1/images
Headers: Không cần (trừ auth).
Kỳ vọng:

Status: 200 OK
Response: JSON array (ví dụ: [{"id": 1, "userId": 1, "imageUrl": "/uploads/xxx.jpg", "createdAt": "2025-10-02T15:00:00"}])
Status: 404 nếu không có ảnh.

Lưu ý: API này hiện tại đã được comment out trong code.

## NGười dùng xóa ảnh
4. Xóa Image (DELETE /api/users/{userId}/image)

Method: DELETE
URL: http://localhost:8081/api/users/1/image
Headers: Thêm Authorization nếu yêu cầu auth.
Kỳ vọng:

Status: 200 OK
Response: "Image deleted successfully"
Status: 404 nếu không có image.

#  Comment, Reaction và Reply Comment
## 1. API Comments
### Tạo comment mới
```
POST /api/comments/posts/{postId}
Parameters:
- userId: Long (required)
- content: String (required)
- emotion: String (optional) - like, love, haha, sad, angry
- fileId: Long (optional)

curl --location 'http://localhost:8081/api/comments/posts/1' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Authorization: Bearer 'Token' \
--data-urlencode 'userId=33' \
--data-urlencode 'content=This is a test comment 2'


```
### Lấy tất cả comments của một post

GET /api/comments/posts/{postId}

### Cập nhật comment
```
PUT /api/comments/{commentId}
Parameters:
- userId: Long (required)
- content: String (required)


```
### Xóa comment

DELETE /api/comments/{commentId}
Parameters:
- userId: Long (required)


## 2. API Reply Comments

### Tạo reply comment
```
POST /api/comments/{commentId}/replies
Parameters:
- content: String (required)
- emotion: String (optional) - like, love, haha, sad, angry
- fileId: Long (optional)

```
### Lấy tất cả reply comments của một comment

GET /api/comments/{commentId}/replies


### Cập nhật reply comment
```
PUT /api/comments/replies/{replyCommentId}
Parameters:
- content: String (required)
- emotion: String (optional)

```
### Xóa reply comment
```
DELETE /api/comments/replies/{replyCommentId}

```
## 3. API Reactions

### Tạo hoặc cập nhật reaction
```
POST /api/reactions/posts/{postId}
Parameters:
- userId: Long (required)
- emotionType: String (required) - like, love, haha, wow, sad, angry

```
### Lấy tất cả reactions của một post

GET /api/reactions/posts/{postId}



### Xóa reaction

DELETE /api/reactions/posts/{postId}
Parameters:
- userId: Long (required)


### Kiểm tra user đã reaction chưa
GET /api/reactions/posts/{postId}/users/{userId}/has-reacted



### Lấy reaction của user cho một post
GET /api/reactions/posts/{postId}/users/{userId}


### Đếm số lượng reaction theo loại
GET /api/reactions/posts/{postId}/count/{emotionType}



# KHÓA HỌC
## Programs APIs
- `GET /api/programs` - Lấy danh sách programs
- `GET /api/programs/{id}` - Lấy chi tiết program với modules
- `POST /api/programs` - Tạo program mới
- `PUT /api/programs/{id}` - Cập nhật program
- `DELETE /api/programs/{id}` - Xóa program
- `POST /api/programs/{id}/modules` - Thêm module vào program

# Modules APIs
- `GET /api/modules` - Lấy danh sách modules
- `GET /api/modules/{id}` - Lấy chi tiết module
- `POST /api/modules` - Tạo module mới
- `PUT /api/modules/{id}` - Cập nhật module
- `DELETE /api/modules/{id}` - Xóa module

## Module Sessions APIs
- `GET /api/modules/{moduleId}/sessions` - Lấy danh sách sessions
- `GET /api/sessions/{sessionId}` - Lấy chi tiết session
- `POST /api/modules/{moduleId}/sessions` - Tạo session mới
- `PUT /api/sessions/{sessionId}` - Cập nhật session
- `DELETE /api/sessions/{sessionId}` - Xóa session

## Classes APIs (Mở rộng)
- `GET /api/classes/{id}/details` - Lấy chi tiết lớp học (giáo viên, chương trình, học viên)
- `PUT /api/classes/{id}/teacher` - Gán/thay đổi giáo viên
- `GET /api/teachers/{teacherId}/classes` - Lấy danh sách lớp của giáo viên




