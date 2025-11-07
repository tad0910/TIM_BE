# ALM-Team-Tim-BE

POST: http://localhost:8081/auth/register
{
"username": "updateduser5",
"password": "123",
"email": "abc5@gmail.com"
}

POST :http://localhost:8081/auth/login
{
"usernameOrEmail": "updateduser5",
"password": "123"
}

GET: http://localhost:8081/users

GET: http://localhost:8081/users/{id}

<!-- GET: http://localhost:8081/users/{email} -->

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

GET http://localhost:8081/profile/users/{id}

# NGười dùng quên mk và yêu cầu reset mk

POST http://localhost:8081/auth/forgot-password

{
"email": "lehbac05@gmail.com",
}

POST http://localhost:8081/auth/verify-otp

{
"email": "lehbac05@gmail.com",
"otp": "654713"
}

POST http://localhost:8081/auth//reset-password

{
"email": "lehbac05@gmail.com",
"reset_token": "HgYvhHNDAm1f9nawYxKYrontS2_VbH7fMuZS4ey48go",
"newPassword": "123456"
}

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

2. Lấy Image (GET /api/users/{userId}/image)

Method: GET
URL: http://localhost:8081/api/users/1/image
Headers: Không cần (trừ auth).
Kỳ vọng:

Status: 200 OK
Response: "/uploads/<uniqueFilename>"
Status: 404 nếu không có image.

3. Lấy Danh sách Ảnh (GET /api/users/{userId}/images) - COMMENTED OUT

Method: GET
URL: http://localhost:8081/api/users/1/images
Headers: Không cần (trừ auth).
Kỳ vọng:

Status: 200 OK
Response: JSON array (ví dụ: [{"id": 1, "userId": 1, "imageUrl": "/uploads/xxx.jpg", "createdAt": "2025-10-02T15:00:00"}])
Status: 404 nếu không có ảnh.

Lưu ý: API này hiện tại đã được comment out trong code.

4. Xóa Image (DELETE /api/users/{userId}/image)

Method: DELETE
URL: http://localhost:8081/api/users/1/image
Headers: Thêm Authorization nếu yêu cầu auth.
Kỳ vọng:

Status: 200 OK
Response: "Image deleted successfully"
Status: 404 nếu không có image.


CREATE TABLE `programs` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`description` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	PRIMARY KEY (`id`) USING BTREE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=12
;


CREATE TABLE `modules` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(255) NOT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`description` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	PRIMARY KEY (`id`) USING BTREE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=4
;


CREATE TABLE `module_sessions` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`module_id` INT NOT NULL,
	`session_number` INT NOT NULL,
	`title` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`content` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`scheduled_at` DATETIME NULL DEFAULT NULL,
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `fk_ms_module` (`module_id`) USING BTREE,
	CONSTRAINT `fk_ms_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
;

CREATE TABLE `program_modules` (
	`program_id` INT NOT NULL,
	`module_id` INT NOT NULL,
	`position` INT NULL DEFAULT NULL,
	PRIMARY KEY (`program_id`, `module_id`) USING BTREE,
	INDEX `fk_pm_module` (`module_id`) USING BTREE,
	CONSTRAINT `fk_pm_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `fk_pm_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
;

CREATE TABLE `classes` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`description` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`program_id` INT NULL DEFAULT NULL,
	`ten_lop` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`mo_ta` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `fk_classes_program` (`program_id`) USING BTREE,
	CONSTRAINT `fk_classes_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON UPDATE CASCADE ON DELETE SET NULL
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=20
;

CREATE TABLE `class_module_schedules` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`class_id` INT NOT NULL,
	`module_id` INT NOT NULL,
	`start_date` DATE NULL DEFAULT NULL,
	`instructor_id` INT NULL DEFAULT NULL,
	`notes` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `fk_cms_class` (`class_id`) USING BTREE,
	INDEX `fk_cms_module` (`module_id`) USING BTREE,
	INDEX `fk_cms_instructor` (`instructor_id`) USING BTREE,
	CONSTRAINT `fk_cms_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `fk_cms_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE SET NULL,
	CONSTRAINT `fk_cms_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=10
;


ALTER TABLE class_module_schedules
ADD COLUMN end_date DATE NULL DEFAULT NULL AFTER start_date,
ADD COLUMN status ENUM('planned','ongoing','completed') NOT NULL DEFAULT 'planned' AFTER end_date;

ALTER TABLE module_sessions
ADD COLUMN end_date DATETIME NULL DEFAULT NULL AFTER scheduled_at,
ADD COLUMN status ENUM('planned','ongoing','completed') NOT NULL DEFAULT 'planned' AFTER end_date;

## Class Module Scheduling & Instructor Assignment

### Luồng gán giảng viên cho Module
- **Endpoint**: `POST /schedules`
- **Payload tối thiểu**:
  ```json
  {
    "classId": 1,
    "moduleId": 5,
    "instructorId": 10,
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  }
  ```
### Gán giáo viên cho từng buổi học (ClassModuleScheduleTeacher)
- **Endpoint chính**: `POST /schedules/{scheduleId}/teachers`
  ```json
  {
    "userId": 45,
    "role": "LECTURER"
  }
  ```
  - Thêm một giáo viên vào buổi học (schedule cụ thể).
  - `role` nhận một trong các giá trị enum: `LECTURER`, `SUPPORTER`, `OBSERVER`.
- **GET /schedules/{scheduleId}/teachers** – danh sách giáo viên của buổi.
- **DELETE /schedules/{scheduleId}/teachers/{userId}** – xoá giáo viên khỏi buổi.
- **PUT /schedules/{scheduleId}/teachers/{userId}/role** – cập nhật vai trò giáo viên trong buổi.

