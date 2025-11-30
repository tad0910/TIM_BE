# Gamification API Documentation

## Tổng quan

Hệ thống Gamification cho phép trao điểm thưởng cho người dùng khi họ hoàn thành các hành vi nhất định, và tự động mở khóa thành tích khi đạt đủ điều kiện. Hệ thống hỗ trợ 3 loại điểm: **Chuyên cần**, **Năng lực**, và **Kinh nghiệm**.

### Cấu trúc dữ liệu:
- **Nhóm hành vi (Behavior Groups)**: Phân loại các hành vi (VD: "Học tập", "Tương tác")
- **Hành vi (Behaviors)**: Các hành vi cụ thể thuộc về một nhóm (VD: "Đăng nhập hàng ngày", "Hoàn thành bài tập")
- Mỗi hành vi phải thuộc về một nhóm hành vi

## Base URL
```
/gamification
```

## Authentication
Tất cả các endpoints đều yêu cầu authentication. Sử dụng JWT token trong header:
```
Authorization: Bearer <token>
```

---

## 1. Point Awarding (Trao điểm thưởng)

### 1.1. Trao điểm thưởng cho user
Trao điểm thưởng khi user hoàn thành một hành vi.

**Endpoint:** `POST /gamification/award-points`

**Authorization:** `gamification:award_points` hoặc `ROLE_ADMIN`, `ROLE_GIAO_VIEN`

**Request Body:**
```json
{
  "userId": 1,
  "behaviorCode": "LOGIN_DAILY"
}
```

**Response 200 OK:**
```json
{
  "userId": 1,
  "pointsDiligenceEarned": 5,
  "pointsCompetenceEarned": 0,
  "pointsExperienceEarned": 2,
  "totalDiligence": 150,
  "totalCompetence": 80,
  "totalExperience": 45,
  "newlyUnlockedAchievements": [
    {
      "id": 10,
      "userId": 1,
      "achievementLevelId": 3,
      "achievementName": "Chiến thần Code dạo",
      "levelName": "Hạng Đồng",
      "unlockedAt": "2024-01-15T10:30:00",
      "isDisplayed": false
    }
  ],
  "message": "Bạn đã nhận được điểm thưởng!"
}
```

**Response 400 Bad Request:**
```json
{
  "error": "Bạn đã đạt giới hạn điểm thưởng cho hành vi này trong khoảng thời gian hiện tại"
}
```

**Response 404 Not Found:**
```json
{
  "error": "Không tìm thấy hành vi với mã: LOGIN_DAILY"
}
```

---

### 1.2. Lấy thống kê điểm của user hiện tại
Lấy thống kê điểm của user đang đăng nhập.

**Endpoint:** `GET /gamification/my-stats`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
{
  "userId": 1,
  "totalDiligence": 150,
  "totalCompetence": 80,
  "totalExperience": 45,
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 1.3. Lấy thống kê điểm của user theo ID
Lấy thống kê điểm của một user cụ thể (admin/teacher hoặc chính user đó).

**Endpoint:** `GET /gamification/users/{userId}/stats`

**Authorization:** `gamification:read_all` hoặc chính user đó

**Path Parameters:**
- `userId` (Long): ID của user

**Response 200 OK:**
```json
{
  "userId": 1,
  "totalDiligence": 150,
  "totalCompetence": 80,
  "totalExperience": 45,
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 1.4. Lấy lịch sử nhận điểm của user hiện tại
Lấy danh sách các lần nhận điểm của user đang đăng nhập.

**Endpoint:** `GET /gamification/my-point-logs`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 100,
    "userId": 1,
    "behaviorId": 5,
    "behaviorCode": "LOGIN_DAILY",
    "behaviorName": "Đăng nhập hàng ngày",
    "pointsDiligenceEarned": 5,
    "pointsCompetenceEarned": 0,
    "pointsExperienceEarned": 2,
    "createdAt": "2024-01-15T08:00:00"
  },
  {
    "id": 99,
    "userId": 1,
    "behaviorId": 3,
    "behaviorCode": "COMPLETE_ASSIGNMENT",
    "behaviorName": "Hoàn thành bài tập",
    "pointsDiligenceEarned": 0,
    "pointsCompetenceEarned": 10,
    "pointsExperienceEarned": 5,
    "createdAt": "2024-01-14T15:30:00"
  }
]
```

---

### 1.5. Lấy lịch sử nhận điểm của user theo ID
Lấy danh sách các lần nhận điểm của một user cụ thể.

**Endpoint:** `GET /gamification/users/{userId}/point-logs`

**Authorization:** `gamification:read_all` hoặc chính user đó

**Path Parameters:**
- `userId` (Long): ID của user

**Response 200 OK:** (Tương tự như endpoint 1.4)

---

### 1.6. Lấy danh sách thành tích của user hiện tại
Lấy danh sách các thành tích đã đạt được của user đang đăng nhập.

**Endpoint:** `GET /gamification/my-achievements`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 10,
    "userId": 1,
    "achievementLevelId": 3,
    "achievementName": "Chiến thần Code dạo",
    "levelName": "Hạng Đồng",
    "unlockedAt": "2024-01-15T10:30:00",
    "isDisplayed": false
  },
  {
    "id": 8,
    "userId": 1,
    "achievementLevelId": 2,
    "achievementName": "Học viên chăm chỉ",
    "levelName": "Hạng Bạc",
    "unlockedAt": "2024-01-10T14:20:00",
    "isDisplayed": true
  }
]
```

---

### 1.7. Lấy danh sách thành tích của user theo ID
Lấy danh sách các thành tích đã đạt được của một user cụ thể.

**Endpoint:** `GET /gamification/users/{userId}/achievements`

**Authorization:** `gamification:read_all` hoặc chính user đó

**Path Parameters:**
- `userId` (Long): ID của user

**Response 200 OK:** (Tương tự như endpoint 1.6)

---

## 2. Point Types Management (Quản lý Loại điểm)

### 2.1. Lấy danh sách tất cả loại điểm đang hoạt động
Lấy danh sách tất cả các loại điểm đang được kích hoạt.

**Endpoint:** `GET /gamification/point-types`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Chuyên cần",
    "description": "Điểm thưởng cho các hành vi chuyên cần như đăng nhập, tham gia lớp học",
    "maxPoints": 0,
    "imageUrl": "https://example.com/images/diligence.png",
    "isActive": true,
    "showOnDashboard": true,
    "createdBy": 1,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 2,
    "name": "Năng lực",
    "description": "Điểm thưởng cho các hành vi thể hiện năng lực như hoàn thành bài tập, làm bài kiểm tra tốt",
    "maxPoints": 0,
    "imageUrl": "https://example.com/images/competence.png",
    "isActive": true,
    "showOnDashboard": true,
    "createdBy": 1,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 3,
    "name": "Kinh nghiệm",
    "description": "Điểm thưởng cho các hành vi tích lũy kinh nghiệm như tham gia dự án, giúp đỡ bạn bè",
    "maxPoints": 0,
    "imageUrl": "https://example.com/images/experience.png",
    "isActive": true,
    "showOnDashboard": true,
    "createdBy": 1,
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

---

### 2.2. Lấy danh sách loại điểm hiển thị trên dashboard
Lấy danh sách các loại điểm được cấu hình để hiển thị trên dashboard.

**Endpoint:** `GET /gamification/point-types/dashboard`

**Authorization:** `isAuthenticated()`

**Response 200 OK:** (Tương tự như endpoint 2.1, nhưng chỉ trả về các loại có `showOnDashboard = true`)

---

### 2.3. Lấy thông tin loại điểm theo ID
Lấy thông tin chi tiết của một loại điểm.

**Endpoint:** `GET /gamification/point-types/{id}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `id` (Integer): ID của loại điểm

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Chuyên cần",
  "description": "Điểm thưởng cho các hành vi chuyên cần",
  "maxPoints": 0,
  "imageUrl": "https://example.com/images/diligence.png",
  "isActive": true,
  "showOnDashboard": true,
  "createdBy": 1,
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 2.4. Tạo loại điểm mới
Tạo một loại điểm mới trong hệ thống.

**Endpoint:** `POST /gamification/point-types`

**Content-Type:** `multipart/form-data`

**Authorization:** `gamification:create`

**Request Body (Form Data):**
- `name` (String, required): Tên loại điểm
- `description` (String, optional): Mô tả
- `maxPoints` (Integer, optional): Điểm tối đa
- `imageFile` (File, optional): **File ảnh để upload** - Key phải là `imageFile`, Type phải là **File**
- `imageUrl` (String, optional): **URL ảnh dạng text** - Key phải là `imageUrl`, Type phải là **Text**
- `isActive` (Boolean, optional): Trạng thái kích hoạt
- `showOnDashboard` (Boolean, optional): Hiển thị trên dashboard
- `createdBy` (Integer, optional): ID người tạo

**Lưu ý quan trọng:**
- Để upload file: Dùng key `imageFile` với Type = **File** trong Postman
- Để gửi URL string: Dùng key `imageUrl` với Type = **Text** trong Postman
- **KHÔNG** dùng key `imageUrl` với Type = File (sẽ gây lỗi)
- Nếu gửi cả `imageFile` và `imageUrl`, `imageFile` sẽ được ưu tiên

**Example using cURL (với file upload):**
```bash
curl -X POST "https://api.example.com/gamification/point-types" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chuyên cần" \
  -F "description=Điểm thưởng cho các hành vi chuyên cần như đăng nhập, tham gia lớp học" \
  -F "maxPoints=0" \
  -F "imageFile=@/path/to/image.png" \
  -F "isActive=true" \
  -F "showOnDashboard=true" \
  -F "createdBy=1"
```

**Example using cURL (với URL string):**
```bash
curl -X POST "https://api.example.com/gamification/point-types" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chuyên cần" \
  -F "description=Điểm thưởng cho các hành vi chuyên cần như đăng nhập, tham gia lớp học" \
  -F "maxPoints=0" \
  -F "imageUrl=https://example.com/images/diligence.png" \
  -F "isActive=true" \
  -F "showOnDashboard=true" \
  -F "createdBy=1"
```

**Response 201 Created:**
```json
{
  "id": 1,
  "name": "Chuyên cần",
  "description": "Điểm thưởng cho các hành vi chuyên cần như đăng nhập, tham gia lớp học",
  "maxPoints": 0,
  "imageUrl": "https://example.com/images/diligence.png",
  "isActive": true,
  "showOnDashboard": true,
  "createdBy": 1,
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### 2.5. Cập nhật loại điểm
Cập nhật thông tin của một loại điểm.

**Endpoint:** `PUT /gamification/point-types/{id}`

**Content-Type:** `multipart/form-data`

**Authorization:** `gamification:update`

**Path Parameters:**
- `id` (Integer): ID của loại điểm

**Request Body (Form Data):**
- `name` (String, optional): Tên loại điểm
- `description` (String, optional): Mô tả
- `maxPoints` (Integer, optional): Điểm tối đa
- `imageFile` (File, optional): **File ảnh để upload** - Key phải là `imageFile`, Type phải là **File**
- `imageUrl` (String, optional): **URL ảnh dạng text** - Key phải là `imageUrl`, Type phải là **Text**
- `isActive` (Boolean, optional): Trạng thái kích hoạt
- `showOnDashboard` (Boolean, optional): Hiển thị trên dashboard

**Lưu ý quan trọng:**
- Để upload file: Dùng key `imageFile` với Type = **File** trong Postman
- Để gửi URL string: Dùng key `imageUrl` với Type = **Text** trong Postman
- **KHÔNG** dùng key `imageUrl` với Type = File (sẽ gây lỗi)
- Nếu gửi cả `imageFile` và `imageUrl`, `imageFile` sẽ được ưu tiên

**Example using cURL (với file upload):**
```bash
curl -X PUT "https://api.example.com/gamification/point-types/1" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chuyên cần (Updated)" \
  -F "description=Mô tả mới" \
  -F "maxPoints=1000" \
  -F "imageFile=@/path/to/image-new.png" \
  -F "isActive=true" \
  -F "showOnDashboard=false"
```

**Example using cURL (với URL string):**
```bash
curl -X PUT "https://api.example.com/gamification/point-types/1" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chuyên cần (Updated)" \
  -F "description=Mô tả mới" \
  -F "maxPoints=1000" \
  -F "imageUrl=https://example.com/images/diligence-new.png" \
  -F "isActive=true" \
  -F "showOnDashboard=false"
```

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Chuyên cần (Updated)",
  "description": "Mô tả mới",
  "maxPoints": 1000,
  "imageUrl": "https://example.com/images/diligence-new.png",
  "isActive": true,
  "showOnDashboard": false,
  "createdBy": 1,
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 2.6. Xóa loại điểm
Xóa một loại điểm khỏi hệ thống.

**Endpoint:** `DELETE /gamification/point-types/{id}`

**Authorization:** `gamification:delete`

**Path Parameters:**
- `id` (Integer): ID của loại điểm

**Response 204 No Content**

---

## 3. Behaviors Management (Quản lý Hành vi)

**Lưu ý:** Mỗi hành vi phải thuộc về một nhóm hành vi. Vui lòng tạo nhóm hành vi trước khi tạo hành vi (xem phần 4. Behavior Groups Management).

### 3.1. Lấy danh sách tất cả hành vi
Lấy danh sách tất cả các hành vi đã được định nghĩa trong hệ thống.

**Endpoint:** `GET /gamification/behaviors`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "groupId": 1,
    "groupName": "Học tập",
    "code": "LOGIN_DAILY",
    "name": "Đăng nhập hàng ngày",
    "frequencyType": "DAILY",
    "maxTimesPerFrequency": 1,
    "pointDiligence": 5,
    "pointCompetence": 0,
    "pointExperience": 2,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 2,
    "groupId": 1,
    "groupName": "Học tập",
    "code": "COMPLETE_ASSIGNMENT",
    "name": "Hoàn thành bài tập",
    "frequencyType": "UNLIMITED",
    "maxTimesPerFrequency": 1,
    "pointDiligence": 0,
    "pointCompetence": 10,
    "pointExperience": 5,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 3,
    "groupId": 2,
    "groupName": "Tương tác",
    "code": "HELP_OTHER",
    "name": "Giúp đỡ bạn bè",
    "frequencyType": "WEEKLY",
    "maxTimesPerFrequency": 5,
    "pointDiligence": 0,
    "pointCompetence": 0,
    "pointExperience": 15,
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

---

### 3.2. Lấy thông tin hành vi theo ID
Lấy thông tin chi tiết của một hành vi.

**Endpoint:** `GET /gamification/behaviors/{id}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `id` (Integer): ID của hành vi

**Response 200 OK:**
```json
{
  "id": 1,
  "groupId": 1,
  "groupName": "Học tập",
  "code": "LOGIN_DAILY",
  "name": "Đăng nhập hàng ngày",
  "frequencyType": "DAILY",
  "maxTimesPerFrequency": 1,
  "pointDiligence": 5,
  "pointCompetence": 0,
  "pointExperience": 2,
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 3.3. Lấy thông tin hành vi theo mã code
Lấy thông tin hành vi bằng mã code (thường dùng khi trao điểm).

**Endpoint:** `GET /gamification/behaviors/code/{code}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `code` (String): Mã code của hành vi (VD: "LOGIN_DAILY")

**Response 200 OK:** (Tương tự như endpoint 3.2)

---

### 3.4. Tạo hành vi mới
Tạo một hành vi mới trong hệ thống.

**Endpoint:** `POST /gamification/behaviors`

**Content-Type:** `application/json`

**Authorization:** `gamification:create`

**Request Body (JSON):**
```json
{
  "groupId": 1,
  "code": "ATTEND_CLASS",
  "name": "Tham gia lớp học",
  "frequencyType": "DAILY",
  "maxTimesPerFrequency": 3,
  "pointDiligence": 10,
  "pointCompetence": 0,
  "pointExperience": 5
}
```

**Response 201 Created:**
```json
{
  "id": 4,
  "groupId": 1,
  "groupName": "Học tập",
  "code": "ATTEND_CLASS",
  "name": "Tham gia lớp học",
  "frequencyType": "DAILY",
  "maxTimesPerFrequency": 3,
  "pointDiligence": 10,
  "pointCompetence": 0,
  "pointExperience": 5,
  "createdAt": "2024-01-15T10:00:00"
}
```

**Lưu ý về frequencyType:**
- `UNLIMITED`: Không giới hạn số lần
- `DAILY`: Giới hạn theo ngày
- `WEEKLY`: Giới hạn theo tuần
- `MONTHLY`: Giới hạn theo tháng
- `ONCE`: Chỉ được nhận điểm một lần duy nhất

---

### 3.5. Cập nhật hành vi
Cập nhật thông tin của một hành vi.

**Endpoint:** `PUT /gamification/behaviors/{id}`

**Content-Type:** `application/json`

**Authorization:** `gamification:update`

**Path Parameters:**
- `id` (Integer): ID của hành vi

**Request Body (JSON):**
```json
{
  "groupId": 1,
  "code": "ATTEND_CLASS",
  "name": "Tham gia lớp học (Updated)",
  "frequencyType": "WEEKLY",
  "maxTimesPerFrequency": 5,
  "pointDiligence": 15,
  "pointCompetence": 0,
  "pointExperience": 8
}
```

**Response 200 OK:** (Tương tự như response của endpoint 3.4)

---

### 3.6. Xóa hành vi
Xóa một hành vi khỏi hệ thống.

**Endpoint:** `DELETE /gamification/behaviors/{id}`

**Authorization:** `gamification:delete`

**Path Parameters:**
- `id` (Integer): ID của hành vi

**Response 204 No Content**

---

## 4. Behavior Groups Management (Quản lý Nhóm hành vi)

### 4.1. Lấy danh sách tất cả nhóm hành vi
Lấy danh sách tất cả các nhóm hành vi đã được định nghĩa trong hệ thống.

**Endpoint:** `GET /gamification/behavior-groups`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Học tập",
    "createdAt": "2024-01-01T00:00:00",
    "behaviors": [
      {
        "id": 1,
        "groupId": 1,
        "groupName": "Học tập",
        "code": "LOGIN_DAILY",
        "name": "Đăng nhập hàng ngày",
        "frequencyType": "DAILY",
        "maxTimesPerFrequency": 1,
        "pointDiligence": 5,
        "pointCompetence": 0,
        "pointExperience": 2,
        "createdAt": "2024-01-01T00:00:00"
      },
      {
        "id": 2,
        "groupId": 1,
        "groupName": "Học tập",
        "code": "COMPLETE_ASSIGNMENT",
        "name": "Hoàn thành bài tập",
        "frequencyType": "UNLIMITED",
        "maxTimesPerFrequency": 1,
        "pointDiligence": 0,
        "pointCompetence": 10,
        "pointExperience": 5,
        "createdAt": "2024-01-01T00:00:00"
      }
    ]
  },
  {
    "id": 2,
    "name": "Tương tác",
    "createdAt": "2024-01-01T00:00:00",
    "behaviors": [
      {
        "id": 3,
        "groupId": 2,
        "groupName": "Tương tác",
        "code": "HELP_OTHER",
        "name": "Giúp đỡ bạn bè",
        "frequencyType": "WEEKLY",
        "maxTimesPerFrequency": 5,
        "pointDiligence": 0,
        "pointCompetence": 0,
        "pointExperience": 15,
        "createdAt": "2024-01-01T00:00:00"
      }
    ]
  },
  {
    "id": 3,
    "name": "Tham gia hoạt động",
    "createdAt": "2024-01-01T00:00:00",
    "behaviors": []
  }
]
```

**Lưu ý:** Mỗi nhóm hành vi sẽ bao gồm danh sách các hành vi (`behaviors`) thuộc nhóm đó. Nếu nhóm chưa có hành vi nào, mảng `behaviors` sẽ là mảng rỗng `[]`.

---

### 4.2. Lấy thông tin nhóm hành vi theo ID
Lấy thông tin chi tiết của một nhóm hành vi.

**Endpoint:** `GET /gamification/behavior-groups/{id}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `id` (Integer): ID của nhóm hành vi

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Học tập",
  "createdAt": "2024-01-01T00:00:00",
  "behaviors": [
    {
      "id": 1,
      "groupId": 1,
      "groupName": "Học tập",
      "code": "LOGIN_DAILY",
      "name": "Đăng nhập hàng ngày",
      "frequencyType": "DAILY",
      "maxTimesPerFrequency": 1,
      "pointDiligence": 5,
      "pointCompetence": 0,
      "pointExperience": 2,
      "createdAt": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "groupId": 1,
      "groupName": "Học tập",
      "code": "COMPLETE_ASSIGNMENT",
      "name": "Hoàn thành bài tập",
      "frequencyType": "UNLIMITED",
      "maxTimesPerFrequency": 1,
      "pointDiligence": 0,
      "pointCompetence": 10,
      "pointExperience": 5,
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

**Lưu ý:** Response bao gồm danh sách tất cả các hành vi (`behaviors`) thuộc nhóm này.

---

### 4.3. Tạo nhóm hành vi mới
Tạo một nhóm hành vi mới trong hệ thống.

**Endpoint:** `POST /gamification/behavior-groups`

**Content-Type:** `application/json`

**Authorization:** `gamification:create`

**Request Body (JSON):**
```json
{
  "name": "Học tập"
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "name": "Học tập",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### 4.4. Cập nhật nhóm hành vi
Cập nhật thông tin của một nhóm hành vi.

**Endpoint:** `PUT /gamification/behavior-groups/{id}`

**Content-Type:** `application/json`

**Authorization:** `gamification:update`

**Path Parameters:**
- `id` (Integer): ID của nhóm hành vi

**Request Body (JSON):**
```json
{
  "name": "Học tập (Updated)"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Học tập (Updated)",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 4.5. Xóa nhóm hành vi
Xóa một nhóm hành vi khỏi hệ thống.

**Endpoint:** `DELETE /gamification/behavior-groups/{id}`

**Authorization:** `gamification:delete`

**Path Parameters:**
- `id` (Integer): ID của nhóm hành vi

**Response 204 No Content**

**Lưu ý:** Nếu nhóm hành vi đang được sử dụng bởi các hành vi, việc xóa có thể gây lỗi. Cần xóa hoặc chuyển các hành vi sang nhóm khác trước khi xóa nhóm.

---

## 5. Achievements Management (Quản lý Thành tích)

### 4.1. Lấy danh sách tất cả thành tích
Lấy danh sách tất cả các thành tích đã được định nghĩa.

**Endpoint:** `GET /gamification/achievements`

**Authorization:** `isAuthenticated()`

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Chiến thần Code dạo",
    "imageUrl": "https://example.com/images/code-warrior.png",
    "createdBy": 1,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 2,
    "name": "Học viên chăm chỉ",
    "imageUrl": "https://example.com/images/diligent-student.png",
    "createdBy": 1,
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

---

### 4.2. Lấy thông tin thành tích theo ID
Lấy thông tin chi tiết của một thành tích.

**Endpoint:** `GET /gamification/achievements/{id}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `id` (Integer): ID của thành tích

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Chiến thần Code dạo",
  "imageUrl": "https://example.com/images/code-warrior.png",
  "createdBy": 1,
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 4.3. Lấy danh sách cấp bậc của thành tích
Lấy danh sách tất cả các cấp bậc của một thành tích (sắp xếp theo điểm yêu cầu tăng dần).

**Endpoint:** `GET /gamification/achievements/{achievementId}/levels`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `achievementId` (Integer): ID của thành tích

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "achievementId": 1,
    "achievementName": "Chiến thần Code dạo",
    "levelName": "Hạng Đồng",
    "requiredPointTypeId": null,
    "requiredPointTypeEnum": "COMPETENCE",
    "minPointsRequired": 100,
    "imageUrl": "https://example.com/images/bronze-badge.png",
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 2,
    "achievementId": 1,
    "achievementName": "Chiến thần Code dạo",
    "levelName": "Hạng Bạc",
    "requiredPointTypeId": null,
    "requiredPointTypeEnum": "COMPETENCE",
    "minPointsRequired": 500,
    "imageUrl": "https://example.com/images/silver-badge.png",
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "id": 3,
    "achievementId": 1,
    "achievementName": "Chiến thần Code dạo",
    "levelName": "Hạng Vàng",
    "requiredPointTypeId": null,
    "requiredPointTypeEnum": "COMPETENCE",
    "minPointsRequired": 1000,
    "imageUrl": "https://example.com/images/gold-badge.png",
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

---

### 4.4. Lấy thông tin cấp bậc thành tích theo ID
Lấy thông tin chi tiết của một cấp bậc thành tích.

**Endpoint:** `GET /gamification/achievement-levels/{id}`

**Authorization:** `isAuthenticated()`

**Path Parameters:**
- `id` (Integer): ID của cấp bậc thành tích

**Response 200 OK:**
```json
{
  "id": 1,
  "achievementId": 1,
  "achievementName": "Chiến thần Code dạo",
  "levelName": "Hạng Đồng",
  "requiredPointTypeId": null,
  "requiredPointTypeEnum": "COMPETENCE",
  "minPointsRequired": 100,
  "imageUrl": "https://example.com/images/bronze-badge.png",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 4.5. Tạo thành tích mới
Tạo một thành tích mới trong hệ thống.

**Endpoint:** `POST /gamification/achievements`

**Content-Type:** `multipart/form-data`

**Authorization:** `gamification:create`

**Request Parameters (Form Data):**
- `name` (String, required): Tên thành tích
- `imageFile` (File, optional): **File ảnh để upload** - Key phải là `imageFile`, Type phải là **File**
- `imageUrl` (String, optional): **URL ảnh dạng text** - Key phải là `imageUrl`, Type phải là **Text**
- `createdBy` (Integer, optional): ID người tạo (mặc định là user hiện tại)

**Lưu ý quan trọng:**
- Để upload file: Dùng key `imageFile` với Type = **File** trong Postman
- Để gửi URL string: Dùng key `imageUrl` với Type = **Text** trong Postman
- **KHÔNG** dùng key `imageUrl` với Type = File (sẽ gây lỗi)
- Nếu gửi cả `imageFile` và `imageUrl`, `imageFile` sẽ được ưu tiên

**Example using cURL (với file upload):**
```bash
curl -X POST "https://api.example.com/gamification/achievements" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chiến thần Code dạo" \
  -F "imageFile=@/path/to/code-warrior.png" \
  -F "createdBy=1"
```

**Example using cURL (với URL string):**
```bash
curl -X POST "https://api.example.com/gamification/achievements" \
  -H "Authorization: Bearer <token>" \
  -F "name=Chiến thần Code dạo" \
  -F "imageUrl=https://example.com/images/code-warrior.png" \
  -F "createdBy=1"
```

**Response 201 Created:**
```json
{
  "id": 1,
  "name": "Chiến thần Code dạo",
  "imageUrl": "https://example.com/images/code-warrior.png",
  "createdBy": 1,
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### 4.6. Tạo cấp bậc thành tích mới
Tạo một cấp bậc mới cho thành tích.

**Endpoint:** `POST /gamification/achievement-levels`

**Content-Type:** `multipart/form-data`

**Authorization:** `gamification:create`

**Request Body (Form Data):**
- `achievementId` (Integer, required): ID của thành tích
- `levelName` (String, required): Tên cấp bậc
- `requiredPointTypeId` (Integer, optional): ID loại điểm yêu cầu
- `requiredPointTypeEnum` (String, optional): Loại điểm yêu cầu (DILIGENCE, COMPETENCE, EXPERIENCE)
- `minPointsRequired` (Integer, optional): Điểm tối thiểu cần có
- `imageFile` (File, optional): **File ảnh để upload** - Key phải là `imageFile`, Type phải là **File**
- `imageUrl` (String, optional): **URL ảnh dạng text** - Key phải là `imageUrl`, Type phải là **Text**

**Lưu ý quan trọng:**
- Để upload file: Dùng key `imageFile` với Type = **File** trong Postman
- Để gửi URL string: Dùng key `imageUrl` với Type = **Text** trong Postman
- **KHÔNG** dùng key `imageUrl` với Type = File (sẽ gây lỗi)
- Nếu gửi cả `imageFile` và `imageUrl`, `imageFile` sẽ được ưu tiên

**Example using cURL (với file upload):**
```bash
curl -X POST "https://api.example.com/gamification/achievement-levels" \
  -H "Authorization: Bearer <token>" \
  -F "achievementId=1" \
  -F "levelName=Hạng Đồng" \
  -F "requiredPointTypeEnum=COMPETENCE" \
  -F "minPointsRequired=100" \
  -F "imageFile=@/path/to/bronze-badge.png"
```

**Example using cURL (với URL string):**
```bash
curl -X POST "https://api.example.com/gamification/achievement-levels" \
  -H "Authorization: Bearer <token>" \
  -F "achievementId=1" \
  -F "levelName=Hạng Đồng" \
  -F "requiredPointTypeEnum=COMPETENCE" \
  -F "minPointsRequired=100" \
  -F "imageUrl=https://example.com/images/bronze-badge.png"
```

**Response 201 Created:**
```json
{
  "id": 1,
  "achievementId": 1,
  "achievementName": "Chiến thần Code dạo",
  "levelName": "Hạng Đồng",
  "requiredPointTypeId": null,
  "requiredPointTypeEnum": "COMPETENCE",
  "minPointsRequired": 100,
  "imageUrl": "https://example.com/images/bronze-badge.png",
  "createdAt": "2024-01-15T10:00:00"
}
```

**Lưu ý về requiredPointTypeEnum:**
- `DILIGENCE`: Yêu cầu điểm Chuyên cần
- `COMPETENCE`: Yêu cầu điểm Năng lực
- `EXPERIENCE`: Yêu cầu điểm Kinh nghiệm

---

### 4.7. Cập nhật cấp bậc thành tích
Cập nhật thông tin của một cấp bậc thành tích.

**Endpoint:** `PUT /gamification/achievement-levels/{id}`

**Content-Type:** `multipart/form-data`

**Authorization:** `gamification:update`

**Path Parameters:**
- `id` (Integer): ID của cấp bậc thành tích

**Request Body (Form Data):**
- `achievementId` (Integer, optional): ID của thành tích
- `levelName` (String, optional): Tên cấp bậc
- `requiredPointTypeId` (Integer, optional): ID loại điểm yêu cầu
- `requiredPointTypeEnum` (String, optional): Loại điểm yêu cầu (DILIGENCE, COMPETENCE, EXPERIENCE)
- `minPointsRequired` (Integer, optional): Điểm tối thiểu cần có
- `imageFile` (File, optional): **File ảnh để upload** - Key phải là `imageFile`, Type phải là **File**
- `imageUrl` (String, optional): **URL ảnh dạng text** - Key phải là `imageUrl`, Type phải là **Text**

**Lưu ý quan trọng:**
- Để upload file: Dùng key `imageFile` với Type = **File** trong Postman
- Để gửi URL string: Dùng key `imageUrl` với Type = **Text** trong Postman
- **KHÔNG** dùng key `imageUrl` với Type = File (sẽ gây lỗi)
- Nếu gửi cả `imageFile` và `imageUrl`, `imageFile` sẽ được ưu tiên

**Example using cURL (với file upload):**
```bash
curl -X PUT "https://api.example.com/gamification/achievement-levels/1" \
  -H "Authorization: Bearer <token>" \
  -F "levelName=Hạng Đồng (Updated)" \
  -F "minPointsRequired=150" \
  -F "imageFile=@/path/to/bronze-badge-new.png"
```

**Example using cURL (với URL string):**
```bash
curl -X PUT "https://api.example.com/gamification/achievement-levels/1" \
  -H "Authorization: Bearer <token>" \
  -F "levelName=Hạng Đồng (Updated)" \
  -F "minPointsRequired=150" \
  -F "imageUrl=https://example.com/images/bronze-badge-new.png"
```

**Response 200 OK:**
```json
{
  "id": 1,
  "achievementId": 1,
  "achievementName": "Chiến thần Code dạo",
  "levelName": "Hạng Đồng (Updated)",
  "requiredPointTypeId": null,
  "requiredPointTypeEnum": "COMPETENCE",
  "minPointsRequired": 150,
  "imageUrl": "https://example.com/images/bronze-badge-new.png",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 4.8. Xóa cấp bậc thành tích
Xóa một cấp bậc thành tích khỏi hệ thống.

**Endpoint:** `DELETE /gamification/achievement-levels/{id}`

**Authorization:** `gamification:delete`

**Path Parameters:**
- `id` (Integer): ID của cấp bậc thành tích

**Response 204 No Content**

---

## 5. Notification Types (Loại thông báo)

Hệ thống tự động gửi thông báo khi:
- User nhận được điểm thưởng (`GAMIFICATION_POINT_EARNED`)
- User đạt được thành tích mới (`GAMIFICATION_ACHIEVEMENT_UNLOCKED`)
- User lên cấp (nếu có) (`GAMIFICATION_LEVEL_UP`)
- User thay đổi thứ hạng trong bảng xếp hạng (`GAMIFICATION_RANKING_CHANGE`)

---

## 6. Ví dụ sử dụng

### Ví dụ 1: Trao điểm khi user đăng nhập hàng ngày

```bash
POST /gamification/award-points
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": 1,
  "behaviorCode": "LOGIN_DAILY"
}
```

### Ví dụ 2: Trao điểm khi user hoàn thành bài tập

```bash
POST /gamification/award-points
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": 1,
  "behaviorCode": "COMPLETE_ASSIGNMENT"
}
```

### Ví dụ 3: Lấy thống kê điểm của user

```bash
GET /gamification/my-stats
Authorization: Bearer <token>
```

### Ví dụ 4: Tạo hành vi mới

```bash
POST /gamification/behaviors
Content-Type: application/json
Authorization: Bearer <token>

{
  "groupId": 1,
  "code": "SUBMIT_PROJECT",
  "name": "Nộp dự án",
  "frequencyType": "UNLIMITED",
  "maxTimesPerFrequency": 1,
  "pointDiligence": 0,
  "pointCompetence": 50,
  "pointExperience": 20
}
```

---

## 7. Database Migration

Để sử dụng tính năng `imageUrl` cho cấp bậc thành tích, bạn cần chạy migration SQL sau:

```sql
ALTER TABLE gamification_achievement_levels 
ADD COLUMN image_url_levels VARCHAR(255) NULL;
```

**Lưu ý:** Tên cột là `image_url_levels` (không phải `image_url`) để tránh trùng với cột `image_url` trong bảng `gamification_achievements`.

---

## 8. Lưu ý quan trọng

1. **Frequency Types:**
   - `UNLIMITED`: Không giới hạn, user có thể nhận điểm bao nhiêu lần cũng được
   - `DAILY`: Giới hạn theo ngày, kiểm tra từ 00:00:00 đến 23:59:59
   - `WEEKLY`: Giới hạn theo tuần, kiểm tra từ thứ 2 đến chủ nhật
   - `MONTHLY`: Giới hạn theo tháng, kiểm tra từ ngày 1 đến ngày cuối tháng
   - `ONCE`: Chỉ được nhận điểm một lần duy nhất trong toàn bộ lịch sử

2. **Tự động mở khóa thành tích:**
   - Hệ thống tự động kiểm tra và mở khóa thành tích mới sau mỗi lần trao điểm
   - Thành tích được mở khóa dựa trên tổng điểm hiện tại của user
   - Mỗi thành tích chỉ được mở khóa một lần

3. **Thông báo:**
   - Thông báo được gửi tự động khi user nhận điểm
   - Thông báo được gửi tự động khi user đạt thành tích mới
   - Thông báo được gửi qua SSE (Server-Sent Events) nếu user đang online

4. **Cache Stats:**
   - Tổng điểm được lưu trong bảng `user_gamification_stats` để tối ưu hiệu suất
   - Stats được cập nhật tự động mỗi khi trao điểm

---

## 9. Error Codes

| Status Code | Mô tả |
|------------|-------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request - Dữ liệu không hợp lệ hoặc đã đạt giới hạn |
| 401 | Unauthorized - Chưa đăng nhập |
| 403 | Forbidden - Không có quyền truy cập |
| 404 | Not Found - Không tìm thấy resource |
| 500 | Internal Server Error - Lỗi server |

---

## 10. Ranking System (Hệ thống Xếp hạng)

Hệ thống xếp hạng cho phép xem bảng xếp hạng người dùng dựa trên điểm gamification. Hệ thống hỗ trợ xếp hạng real-time và xếp hạng theo tháng (snapshot).

### Base URL
```
/gamification/ranking
```

### 10.1. Lấy bảng xếp hạng hiện tại (Real-time)

Lấy bảng xếp hạng real-time, được tính toán dựa trên điểm hiện tại của người dùng.

**Endpoint:** `GET /gamification/ranking/current`

**Authorization:** `isAuthenticated()`

**Query Parameters:**
| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `classId` | Long | No | null | Lọc theo lớp học (null = tất cả) |
| `sortBy` | String | No | "experience" | Cách sắp xếp: "experience", "competence", "diligence", "total" |
| `page` | Integer | No | 0 | Số trang (bắt đầu từ 0) |
| `size` | Integer | No | 100 | Số lượng kết quả mỗi trang |

**Response 200 OK:**
```json
{
  "rankings": [
    {
      "userId": 1,
      "username": "student1",
      "displayName": "Nguyễn Văn A",
      "profileImage": "https://example.com/avatar.jpg",
      "totalDiligenceScore": 150,
      "totalCompetenceScore": 200,
      "totalExperienceScore": 300,
      "rankPosition": 1,
      "classId": 1,
      "className": "Lớp Java 2024"
    },
    {
      "userId": 2,
      "username": "student2",
      "displayName": "Trần Thị B",
      "profileImage": "https://example.com/avatar2.jpg",
      "totalDiligenceScore": 120,
      "totalCompetenceScore": 180,
      "totalExperienceScore": 280,
      "rankPosition": 2,
      "classId": 1,
      "className": "Lớp Java 2024"
    }
  ],
  "total": 50,
  "page": 0,
  "size": 100,
  "sortBy": "experience"
}
```

**Ví dụ Request:**
```bash
GET /gamification/ranking/current?classId=1&sortBy=experience&page=0&size=20
Authorization: Bearer <token>
```

---

### 10.2. Lấy bảng xếp hạng theo tháng

Lấy bảng xếp hạng snapshot của một tháng cụ thể (đã được lưu trước đó).

**Endpoint:** `GET /gamification/ranking/monthly`

**Authorization:** `isAuthenticated()`

**Query Parameters:**
| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `monthYear` | String | **Yes** | - | Tháng/năm (format: "YYYY-MM", ví dụ: "2025-11") |
| `classId` | Long | No | null | Lọc theo lớp học (null = tất cả) |
| `sortBy` | String | No | "experience" | Cách sắp xếp: "experience", "competence", "diligence", "total" |
| `page` | Integer | No | 0 | Số trang (bắt đầu từ 0) |
| `size` | Integer | No | 100 | Số lượng kết quả mỗi trang |

**Response 200 OK:**
```json
{
  "rankings": [
    {
      "userId": 1,
      "username": "student1",
      "displayName": "Nguyễn Văn A",
      "profileImage": "https://example.com/avatar.jpg",
      "totalDiligenceScore": 150,
      "totalCompetenceScore": 200,
      "totalExperienceScore": 300,
      "rankPosition": 1,
      "classId": 1,
      "className": "Lớp Java 2024"
    }
  ],
  "total": 50,
  "page": 0,
  "size": 100,
  "sortBy": "experience",
  "monthYear": "2025-11"
}
```

**Ví dụ Request:**
```bash
GET /gamification/ranking/monthly?monthYear=2025-11&classId=1&sortBy=experience
Authorization: Bearer <token>
```

---

### 10.3. Lấy vị trí xếp hạng của user hiện tại

Lấy vị trí xếp hạng của user đang đăng nhập (real-time hoặc theo tháng).

**Endpoint:** `GET /gamification/ranking/my-position`

**Authorization:** `isAuthenticated()`

**Query Parameters:**
| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `monthYear` | String | No | null | Tháng/năm (format: "YYYY-MM"). Nếu null = real-time |
| `classId` | Long | No | null | Lọc theo lớp học (null = tất cả) |
| `sortBy` | String | No | "experience" | Cách sắp xếp: "experience", "competence", "diligence", "total" |

**Response 200 OK:**
```json
{
  "userId": 1,
  "username": "student1",
  "totalDiligenceScore": 150,
  "totalCompetenceScore": 200,
  "totalExperienceScore": 300,
  "rankPosition": 5,
  "totalUsers": 50,
  "monthYear": null,
  "classId": 1,
  "className": "Lớp Java 2024"
}
```

**Response 404 Not Found:**
```json
{
  "error": "Not Found",
  "message": "User ranking not found"
}
```

**Ví dụ Request:**
```bash
# Lấy vị trí real-time
GET /gamification/ranking/my-position?sortBy=experience
Authorization: Bearer <token>

# Lấy vị trí theo tháng
GET /gamification/ranking/my-position?monthYear=2025-11&sortBy=experience
Authorization: Bearer <token>
```

---

### 10.4. Tạo snapshot xếp hạng theo tháng (Admin only)

Tạo snapshot xếp hạng cho tháng hiện tại hoặc tháng chỉ định. Snapshot này sẽ được lưu vào bảng `ranking_monthly` để có thể xem lại sau.

**Endpoint:** `POST /gamification/ranking/snapshot`

**Authorization:** `gamification:create` (Admin only)

**Query Parameters:**
| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `monthYear` | String | No | current month | Tháng/năm (format: "YYYY-MM"). Nếu null = tháng hiện tại |

**Response 200 OK:**
```
(No content)
```

**Ví dụ Request:**
```bash
# Tạo snapshot cho tháng hiện tại
POST /gamification/ranking/snapshot
Authorization: Bearer <token>

# Tạo snapshot cho tháng cụ thể
POST /gamification/ranking/snapshot?monthYear=2025-10
Authorization: Bearer <token>
```

**Lưu ý:**
- Snapshot được tạo dựa trên điểm hiện tại của người dùng tại thời điểm tạo snapshot
- Nếu snapshot đã tồn tại cho tháng đó, nó sẽ được cập nhật
- Snapshot được sắp xếp theo **Experience** (Option 1) theo mặc định

---

### 10.5. Các cách sắp xếp (sortBy)

| Giá trị | Mô tả | Thứ tự ưu tiên |
|---------|-------|----------------|
| `experience` | Xếp hạng theo Experience (khuyến nghị) | Experience DESC → Competence DESC → Diligence DESC → UserId ASC |
| `competence` | Xếp hạng theo Competence | Competence DESC → Experience DESC → Diligence DESC → UserId ASC |
| `diligence` | Xếp hạng theo Diligence | Diligence DESC → Experience DESC → Competence DESC → UserId ASC |
| `total` | Xếp hạng theo tổng điểm | (Diligence + Competence + Experience) DESC → UserId ASC |

**Mặc định:** `experience` (Option 1 - Khuyến nghị)

---

## 11. Integration với các service khác

Để tích hợp hệ thống Gamification vào các service khác, bạn có thể gọi `GamificationService.awardPoints()` từ bất kỳ service nào:

```java
@Autowired
private GamificationService gamificationService;

public void onUserLogin(Long userId) {
    try {
        gamificationService.awardPoints(userId, "LOGIN_DAILY");
    } catch (Exception e) {
        // Log error nhưng không làm gián đoạn flow chính
        log.error("Failed to award points for login", e);
    }
}
```

---

---

## 12. Database Migration cho Ranking

### 12.1. Cập nhật bảng ranking

Chạy file `DB/update_ranking_table.sql` để cập nhật cấu trúc bảng `ranking`:
- Bỏ cột `diem_tong_hop` và `program_id`
- Thêm các cột `total_diligence_score`, `total_competence_score`, `total_experience_score`

### 12.2. Tạo bảng ranking_monthly

Chạy file `DB/create_ranking_monthly_table.sql` để tạo bảng `ranking_monthly` cho việc lưu snapshot xếp hạng theo tháng.

---

**Tài liệu này được cập nhật lần cuối:** 2025-11-30

