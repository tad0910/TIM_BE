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


# Hướng Dẫn: Thêm Giáo Viên Vào Module và Module Session

## Tổng Quan

Giải pháp này cho phép bạn gán giáo viên (instructor) cho:
1. **Module**: Gán giáo viên cho cả một module
2. **Module Session**: Gán giáo viên cho từng buổi học cụ thể

## Cấu Trúc Database

### Thay Đổi Schema

Đã thêm cột `instructor_id` vào:
- Bảng `modules`: Gán giáo viên mặc định cho module
- Bảng `module_sessions`: Gán giáo viên cho từng buổi học

### Migration

Chạy file migration SQL:
```bash
mysql -u your_user -p dbtest < DB/add-instructor-to-modules.sql
```

Hoặc thực thi file: `DB/add-instructor-to-modules.sql`

## API Endpoints

### 1. Gán Giáo Viên Cho Module

#### Endpoint riêng để gán giáo viên:
```
PUT /module/{moduleId}/instructor?instructorId={instructorId}
```

**Request:**
- `moduleId`: ID của module (path parameter)
- `instructorId`: ID của giáo viên (query parameter, có thể null để xóa giáo viên)

**Example:**
```bash
PUT /module/1/instructor?instructorId=2
```

#### Hoặc gán khi tạo/cập nhật module:
```
POST /module
PUT /module/{id}
```

**Request Body:**
```json
{
  "name": "Module Name",
  "description": "Module Description",
  "instructorId": 2  // Optional
}
```

### 2. Gán Giáo Viên Cho Module Session

#### Endpoint riêng để gán giáo viên:
```
PUT /modules/sessions/{sessionId}/instructor?instructorId={instructorId}
```

**Request:**
- `sessionId`: ID của buổi học (path parameter)
- `instructorId`: ID của giáo viên (query parameter, có thể null để xóa giáo viên)

**Example:**
```bash
PUT /modules/sessions/10/instructor?instructorId=2
```

#### Hoặc gán khi tạo/cập nhật session:
```
POST /modules/{moduleId}/sessions
PUT /modules/sessions/{sessionId}
```

**Request Body:**
```json
{
  "sessionNumber": 1,
  "title": "Session Title",
  "content": "Session Content",
  "scheduledAt": "2024-01-01T10:00:00",
  "endDate": "2024-01-01T12:00:00",
  "status": "planned",
  "instructorId": 2  // Optional - nếu không có, sẽ dùng instructor của module
}
```


# Sử dụng DB

## Tạo db mới
mysql -u root -p < DB/create-database.sql

## INSERT DB
mysql -u root -p < DB/sample-data.sql


# JSON Examples để Test các Chức năng Gán Giáo viên trên Postman

## Base URL
```
http://localhost:8080
```

## 1. GÁN GIÁO VIÊN VÀO CLASS MODULE (ClassModuleTeacher)

### 1.1. Tạo ClassModule từ Program của lớp
**POST** `/classes/{classId}/modules/from-program`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `classId`: 1 (ví dụ)

**Response:**
```json
[
  {
    "id": 1,
    "classId": 1,
    "className": "Lớp Frontend 2024",
    "moduleId": 1,
    "moduleName": "ReactJS",
    "scheduleType": "fixed",
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

---

### 1.2. Tạo ClassModule thủ công
**POST** `/classes/{classId}/modules`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `classId`: 1

**Body (JSON):**
```json
{
  "moduleId": 2,
  "scheduleType": "flexible"
}
```

**Các giá trị scheduleType hợp lệ:**
- `"fixed"`
- `"flexible"`
- `"online"`
- `"offline"`

**Response:**
```json
{
  "id": 2,
  "classId": 1,
  "className": "Lớp Frontend 2024",
  "moduleId": 2,
  "moduleName": "VueJS",
  "scheduleType": "flexible",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 1.3. Gán giáo viên vào ClassModule
**POST** `/classes/{classId}/modules/{classModuleId}/teachers`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `classId`: 1
- `classModuleId`: 1

**Body (JSON):**
```json
{
  "userId": 5,
  "role": "MAIN"
}
```

**Các giá trị role hợp lệ:**
- `"MAIN"` - Giáo viên chính
- `"ASSISTANT"` - Giáo viên phụ
- `"MENTOR"` - Giáo viên hướng dẫn

**Response:**
```json
{
  "id": 1,
  "classModuleId": 1,
  "userId": 5,
  "userName": "teacher1",
  "userEmail": "teacher1@example.com",
  "role": "MAIN",
  "assignedAt": "2024-01-15T11:00:00"
}
```

**Ví dụ gán giáo viên phụ:**
```json
{
  "userId": 6,
  "role": "ASSISTANT"
}
```

---

### 1.4. Lấy danh sách giáo viên của ClassModule
**GET** `/classes/{classId}/modules/{classModuleId}/teachers`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `classId`: 1
- `classModuleId`: 1

**Response:**
```json
[
  {
    "id": 1,
    "classModuleId": 1,
    "userId": 5,
    "userName": "teacher1",
    "userEmail": "teacher1@example.com",
    "role": "MAIN",
    "assignedAt": "2024-01-15T11:00:00"
  },
  {
    "id": 2,
    "classModuleId": 1,
    "userId": 6,
    "userName": "teacher2",
    "userEmail": "teacher2@example.com",
    "role": "ASSISTANT",
    "assignedAt": "2024-01-15T11:05:00"
  }
]
```

---

### 1.5. Cập nhật vai trò giáo viên trong ClassModule
**PUT** `/classes/{classId}/modules/{classModuleId}/teachers/{userId}/role`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `classId`: 1
- `classModuleId`: 1
- `userId`: 6

**Body (JSON):**
```json
{
  "role": "MENTOR"
}
```

**Response:**
```json
{
  "id": 2,
  "classModuleId": 1,
  "userId": 6,
  "userName": "teacher2",
  "userEmail": "teacher2@example.com",
  "role": "MENTOR",
  "assignedAt": "2024-01-15T11:05:00"
}
```

---

### 1.6. Xóa giáo viên khỏi ClassModule
**DELETE** `/classes/{classId}/modules/{classModuleId}/teachers/{userId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `classId`: 1
- `classModuleId`: 1
- `userId`: 6

**Response:**
```json
{
  "message": "Xóa giáo viên khỏi ClassModule thành công"
}
```

---

## 2. GÁN GIÁO VIÊN VÀO BUỔI HỌC (ClassModuleScheduleTeacher)

### 2.1. Tạo lịch học (Schedule)
**POST** `/schedules`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "classId": 1,
  "moduleId": 1,
  "classModuleId": 1,
  "moduleSessionId": 5,
  "startDate": "2024-02-01",
  "endDate": "2024-02-05",
  "instructorId": 5,
  "status": "planned"
}
```

**Response:**
```json
{
  "id": 1,
  "classId": 1,
  "className": "Lớp Frontend 2024",
  "moduleId": 1,
  "moduleName": "ReactJS",
  "classModuleId": 1,
  "moduleSessionId": 5,
  "instructorId": 5,
  "instructorName": "teacher1",
  "startDate": "2024-02-01",
  "endDate": "2024-02-05",
  "status": "planned"
}
```

---

### 2.2. Gán giáo viên vào buổi học
**POST** `/schedules/{scheduleId}/teachers`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `scheduleId`: 1

**Body (JSON):**
```json
{
  "userId": 7,
  "role": "LECTURER"
}
```

**Các giá trị role hợp lệ:**
- `"LECTURER"` - Giảng viên (mặc định)
- `"SUPPORTER"` - Giáo viên hỗ trợ
- `"OBSERVER"` - Người quan sát

**Response:**
```json
{
  "id": 1,
  "classModuleScheduleId": 1,
  "userId": 7,
  "userName": "teacher3",
  "userEmail": "teacher3@example.com",
  "role": "LECTURER",
  "assignedAt": "2024-01-15T12:00:00"
}
```

**Ví dụ gán giáo viên hỗ trợ:**
```json
{
  "userId": 8,
  "role": "SUPPORTER"
}
```

**Ví dụ gán người quan sát:**
```json
{
  "userId": 9,
  "role": "OBSERVER"
}
```

---

### 2.3. Lấy danh sách giáo viên của buổi học
**GET** `/schedules/{scheduleId}/teachers`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `scheduleId`: 1

**Response:**
```json
[
  {
    "id": 1,
    "classModuleScheduleId": 1,
    "userId": 7,
    "userName": "teacher3",
    "userEmail": "teacher3@example.com",
    "role": "LECTURER",
    "assignedAt": "2024-01-15T12:00:00"
  },
  {
    "id": 2,
    "classModuleScheduleId": 1,
    "userId": 8,
    "userName": "teacher4",
    "userEmail": "teacher4@example.com",
    "role": "SUPPORTER",
    "assignedAt": "2024-01-15T12:05:00"
  }
]
```

---

### 2.4. Cập nhật vai trò giáo viên trong buổi học
**PUT** `/schedules/{scheduleId}/teachers/{userId}/role`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Variables:**
- `scheduleId`: 1
- `userId`: 8

**Body (JSON):**
```json
{
  "role": "LECTURER"
}
```

**Response:**
```json
{
  "id": 2,
  "classModuleScheduleId": 1,
  "userId": 8,
  "userName": "teacher4",
  "userEmail": "teacher4@example.com",
  "role": "LECTURER",
  "assignedAt": "2024-01-15T12:05:00"
}
```

---

### 2.5. Xóa giáo viên khỏi buổi học
**DELETE** `/schedules/{scheduleId}/teachers/{userId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `scheduleId`: 1
- `userId`: 8

**Response:**
```json
{
  "message": "Xóa giáo viên khỏi buổi học thành công"
}
```

---

## 3. LẤY LỊCH HỌC CỦA GIÁO VIÊN

### 3.1. Lấy lịch học của giáo viên (vai trò chính)
**GET** `/schedules/instructor/{instructorId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `instructorId`: 5

**Query Parameters (optional):**
- `startDate`: 2024-01-01
- `endDate`: 2024-12-31

**Example:**
```
GET /schedules/instructor/5?startDate=2024-01-01&endDate=2024-12-31
```

**Response:**
```json
[
  {
    "id": 1,
    "classId": 1,
    "className": "Lớp Frontend 2024",
    "moduleId": 1,
    "moduleName": "ReactJS",
    "classModuleId": 1,
    "moduleSessionId": 5,
    "instructorId": 5,
    "instructorName": "teacher1",
    "startDate": "2024-02-01",
    "endDate": "2024-02-05",
    "status": "planned"
  }
]
```

---

### 3.2. Lấy TẤT CẢ lịch học của giáo viên (chính + phụ)
**GET** `/schedules/teacher/{teacherId}/all`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `teacherId`: 7

**Query Parameters (optional):**
- `startDate`: 2024-01-01
- `endDate`: 2024-12-31

**Example:**
```
GET /schedules/teacher/7/all?startDate=2024-01-01&endDate=2024-12-31
```

**Response:**
```json
[
  {
    "id": 1,
    "classId": 1,
    "className": "Lớp Frontend 2024",
    "moduleId": 1,
    "moduleName": "ReactJS",
    "classModuleId": 1,
    "moduleSessionId": 5,
    "instructorId": 5,
    "instructorName": "teacher1",
    "startDate": "2024-02-01",
    "endDate": "2024-02-05",
    "status": "planned"
  },
  {
    "id": 2,
    "classId": 2,
    "className": "Lớp Backend 2024",
    "moduleId": 3,
    "moduleName": "Spring Boot",
    "classModuleId": 3,
    "moduleSessionId": 8,
    "instructorId": 6,
    "instructorName": "teacher2",
    "startDate": "2024-02-10",
    "endDate": "2024-02-15",
    "status": "planned"
  }
]
```

---

## 4. CÁC API KHÁC HỮU ÍCH

### 4.1. Lấy danh sách ClassModule của lớp
**GET** `/classes/{classId}/modules`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `classId`: 1

---

### 4.2. Lấy chi tiết ClassModule
**GET** `/classes/{classId}/modules/{classModuleId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `classId`: 1
- `classModuleId`: 1

---

### 4.3. Lấy lịch học của lớp
**GET** `/schedules/class/{classId}`

**Headers:**
```
Authorization: Bearer {token}
```

**Path Variables:**
- `classId`: 1

**Query Parameters (optional):**
- `startDate`: 2024-01-01
- `endDate`: 2024-12-31

---

## LƯU Ý

1. **Authentication**: Tất cả các API đều cần token JWT trong header `Authorization: Bearer {token}`

2. **Permissions**: 
   - Các API tạo/sửa/xóa cần quyền `class:update_all` hoặc `schedule:update`
   - Các API đọc cần `isAuthenticated()` hoặc quyền tương ứng

3. **Enum Values**:
   - **ClassModuleTeacher.role**: `MAIN`, `ASSISTANT`, `MENTOR`
   - **ClassModuleScheduleTeacher.role**: `LECTURER`, `SUPPORTER`, `OBSERVER`
   - **ClassModule.scheduleType**: `fixed`, `flexible`, `online`, `offline`
   - **ClassModuleSchedule.status**: `planned`, `ongoing`, `completed`

4. **Date Format**: Sử dụng format `YYYY-MM-DD` (ví dụ: `2024-02-01`)

5. **ID Types**: 
   - `classId`, `userId`, `scheduleId`: Long (số nguyên)
   - `moduleId`: Integer (số nguyên)


