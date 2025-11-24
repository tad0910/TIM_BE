-- 1. Thêm Roles mới vào bảng roles
INSERT IGNORE INTO roles (id, name) VALUES 
  (4, 'ROLE_KE_TOAN'),
  (5, 'ROLE_GIAO_VU');

  CREATE TABLE IF NOT EXISTS `form_templates` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã code để dev xử lý logic (VD: FORM_BAO_LUU)',
  `name` VARCHAR(100) NOT NULL COMMENT 'Tên hiển thị (VD: Đơn bảo lưu)',
  `description` TEXT COMMENT 'Mô tả quy định, hướng dẫn điền đơn này',
  `is_active` BOOLEAN DEFAULT TRUE COMMENT 'Còn sử dụng hay không'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dữ liệu mẫu cho 4 loại đơn bạn đã cung cấp
INSERT INTO `form_templates` (`code`, `name`, `description`) VALUES 
('RESERVATION', 'Đơn Bảo lưu', 'Dùng cho HV tạm dừng học có thời hạn (Start date - End date)'),
('DROPOUT', 'Đơn Thôi học', 'Dùng cho HV nghỉ hẳn, có tính toán hoàn phí'),
('SUSPENSION', 'Quyết định Đình chỉ', 'Dùng cho HV bị buộc thôi học do vi phạm'),
('TRANSFER', 'Đơn Chuyển lớp', 'Dùng cho HV chuyển sang lớp khác hoặc đổi chương trình học');

USE dbtest;

CREATE TABLE IF NOT EXISTS `student_forms` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` INT NOT NULL,
  `student_id` BIGINT NOT NULL COMMENT 'Học viên làm đơn',
  `class_id` BIGINT NOT NULL COMMENT 'Lớp hiện tại',
  `created_by_user_id` BIGINT NOT NULL COMMENT 'Người lập đơn (Giáo vụ/Admin)',

  `reason` TEXT,
  `start_date` DATE,
  `end_date` DATE,
  `decision_date` DATE,
  `target_class_id` BIGINT,
  `target_program_type` VARCHAR(100),
  `fee_amount` DECIMAL(15,2) DEFAULT 0,
  `payment_status` ENUM('NOT_REQUIRED','PENDING','PAID','REFUNDED') DEFAULT 'NOT_REQUIRED',

  `coach_approval` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `coach_note` TEXT,
  `coach_user_id` BIGINT DEFAULT NULL COMMENT 'Lưu ID của Giáo viên đã duyệt',

  `academic_approval` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `academic_note` TEXT,
  `academic_user_id` BIGINT DEFAULT NULL COMMENT 'Lưu ID của Giáo vụ đã duyệt',

  `accountant_approval` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `accountant_note` TEXT,
  `accountant_user_id` BIGINT DEFAULT NULL COMMENT 'Lưu ID của Kế toán đã duyệt',

  `admin_approval` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  `admin_note` TEXT,
  `admin_user_id` BIGINT DEFAULT NULL COMMENT 'Lưu ID của Giám đốc đã duyệt',

  `status` ENUM('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (`template_id`) REFERENCES `form_templates`(`id`),
  FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`coach_user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`academic_user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`accountant_user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`admin_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4;