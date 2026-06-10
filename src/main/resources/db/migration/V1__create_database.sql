CREATE DATABASE IF NOT EXISTS dbtest;
USE dbtest;

CREATE TABLE IF NOT EXISTS `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `firstname` varchar(255) DEFAULT NULL,
  `lastname` varchar(255) DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `so_dien_thoai` varchar(255) DEFAULT NULL,
  `anh_dai_dien` varchar(255) DEFAULT NULL,
  `vai_tro` enum('sinh_vien','giao_vien','admin') DEFAULT NULL,
  `ngay_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `login` varchar(255) DEFAULT NULL,
  `password_changed_at` datetime(6) DEFAULT NULL,
  `keycloak_id` varchar(255) DEFAULT NULL,
  `refresh_token` varchar(500) DEFAULT NULL,
  `refresh_token_expiry` datetime(6) DEFAULT NULL,
  `anh_bia` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `UK366dgrd625s5659shyen79mmw` (`keycloak_id`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nguoi_dung_id` int NOT NULL,
  `url_anh` varchar(255) DEFAULT NULL,
  `mo_ta` varchar(255) DEFAULT NULL,
  `thoi_gian_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_user_images_user` (`nguoi_dung_id`),
  CONSTRAINT `fk_user_images_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `programs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `posts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nguoi_dung_id` int NOT NULL,
  `noi_dung` varchar(255) DEFAULT NULL,
  `quyen_rieng_tu` enum('open','friends','only_me') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'open',
  `thoi_gian_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `thoi_gian_cap_nhat` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `tong_reactions` int DEFAULT '0',
  `tong_comments` int DEFAULT '0',
  `link_description` varchar(1000) DEFAULT NULL,
  `link_domain` varchar(200) DEFAULT NULL,
  `link_image_url` varchar(500) DEFAULT NULL,
  `link_title` varchar(500) DEFAULT NULL,
  `link_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_posts_user` (`nguoi_dung_id`),
  KEY `idx_posts_link_url` (`link_url`),
  CONSTRAINT `fk_posts_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `password_reset_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `otp_hash` varchar(255) DEFAULT NULL,
  `token_type` enum('OTP','LINK') DEFAULT 'OTP',
  `attempts` int DEFAULT '0',
  `used` tinyint(1) DEFAULT '0',
  `expires_at` datetime NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `request_ip` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `password_reset_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` int NOT NULL,
  `sender_id` int DEFAULT NULL,
  `notification_type` enum('POST_REACTION', 'POST_COMMENT', 'COMMENT_REACTION', 'COMMENT_REPLY',
                          'REPLY_REACTION', 'USER_FOLLOW', 'POST_MENTION', 'COMMENT_MENTION',
                          'SYSTEM_ANNOUNCEMENT', 'GRADE_NEW', 'GRADE_UPDATED',
                          'BLOG_NEW', 'LATE_ATTENDANCE_OPENED',
                          'ATTENDANCE_REMINDER_LATE','ATTENDANCE_REMINDER_ENDING')
   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `content` text,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notification_receiver` (`receiver_id`),
  KEY `fk_notification_sender` (`sender_id`),
  CONSTRAINT `fk_notification_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `modules` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `module_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_id` int NOT NULL,
  `session_number` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `fk_ms_module` (`module_id`),
  CONSTRAINT `fk_ms_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `bai_viet_id` int NOT NULL,
  `nguoi_dung_id` int NOT NULL,
  `noi_dung` varchar(255) DEFAULT NULL,
  `thoi_gian_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `emotion` enum('like','love','haha','sad','angry') DEFAULT NULL,
  `files_id` bigint DEFAULT NULL,
  `reaction_id` bigint DEFAULT NULL,
  `tong_reaction` int NOT NULL DEFAULT '0',
  `thoi_gian_cap_nhat` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_comments_post` (`bai_viet_id`),
  KEY `fk_comments_user` (`nguoi_dung_id`),
  CONSTRAINT `fk_comments_post` FOREIGN KEY (`bai_viet_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `classes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `program_id` int DEFAULT NULL,
  `ten_lop` varchar(255) DEFAULT NULL,
  `mo_ta` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_classes_program` (`program_id`),
  CONSTRAINT `fk_classes_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `class_module_schedules` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`class_id` INT NOT NULL,
	`module_id` INT NOT NULL,
	`class_module_id` BIGINT NULL DEFAULT NULL,
	`start_date` DATETIME NULL DEFAULT NULL,
	`end_date` DATETIME NULL DEFAULT NULL,
	`status` ENUM('planned','ongoing','completed') NOT NULL DEFAULT 'planned' COLLATE 'utf8mb4_0900_ai_ci',
	`instructor_id` INT NULL DEFAULT NULL,
	`notes` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`module_session_id` BIGINT NULL DEFAULT NULL,
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `fk_cms_class` (`class_id`) USING BTREE,
	INDEX `fk_cms_module` (`module_id`) USING BTREE,
	INDEX `fk_cms_instructor` (`instructor_id`) USING BTREE,
	INDEX `fk_cms_module_session` (`module_session_id`) USING BTREE,
	INDEX `fk_cms_class_module` (`class_module_id`) USING BTREE,
	CONSTRAINT `fk_cms_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `fk_cms_class_module` FOREIGN KEY (`class_module_id`) REFERENCES `class_module` (`id`) ON UPDATE CASCADE ON DELETE SET NULL,
	CONSTRAINT `fk_cms_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE ON DELETE SET NULL,
	CONSTRAINT `fk_cms_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `fk_cms_module_session` FOREIGN KEY (`module_session_id`) REFERENCES `module_sessions` (`id`) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
;

CREATE TABLE IF NOT EXISTS `class_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lop_id` int NOT NULL,
  `nguoi_dung_id` int NOT NULL,
  `vai_tro` enum('sinh_vien','giao_vien') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `ngay_tham_gia` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_class_members_class` (`lop_id`),
  KEY `fk_class_members_user` (`nguoi_dung_id`),
  CONSTRAINT `fk_class_members_class` FOREIGN KEY (`lop_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_class_members_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `reply_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comments_id` int NOT NULL,
  `noi_dung` varchar(255) DEFAULT NULL,
  `thoi_gian_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `emotion` enum('like','love','haha','sad','angry') DEFAULT NULL,
  `files_id` bigint DEFAULT NULL,
  `nguoi_dung_id` bigint NOT NULL,
  `tong_reaction` int NOT NULL DEFAULT '0',
  `thoi_gian_cap_nhat` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_reply_comments_comment` (`comments_id`),
  CONSTRAINT `fk_reply_comments_comment` FOREIGN KEY (`comments_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



CREATE TABLE IF NOT EXISTS `role_permissions` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `FKegdk29eiy7mdtefy5c7eirr6e` (`permission_id`),
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `reactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bai_viet_id` int DEFAULT NULL,
  `nguoi_dung_id` int DEFAULT NULL,
  `loai_cam_xuc` enum('like','love','haha','wow','sad','angry') DEFAULT NULL,
  `thoi_gian_tao` datetime DEFAULT CURRENT_TIMESTAMP,
  `comment_id` int DEFAULT NULL,
  `reply_comment_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_reactions_post` (`bai_viet_id`),
  KEY `fk_reactions_user` (`nguoi_dung_id`),
  KEY `FK4twk7tpc7qcki5i92xg0fowaq` (`reply_comment_id`),
  KEY `fk_reactions_comment` (`comment_id`),
  CONSTRAINT `FK4twk7tpc7qcki5i92xg0fowaq` FOREIGN KEY (`reply_comment_id`) REFERENCES `reply_comments` (`id`),
  CONSTRAINT `fk_reactions_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_reactions_post` FOREIGN KEY (`bai_viet_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_reactions_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ranking` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nguoi_dung_id` int NOT NULL,
  `diem_tong_hop` int DEFAULT NULL,
  `classes_id` int DEFAULT NULL,
  `program_id` int DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `courses_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_ranking_user` (`nguoi_dung_id`),
  KEY `fk_ranking_class` (`classes_id`),
  KEY `fk_ranking_program` (`program_id`),
  CONSTRAINT `fk_ranking_class` FOREIGN KEY (`classes_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ranking_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ranking_user` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `invalidated_tokens` (
  `jti` varchar(255) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  PRIMARY KEY (`jti`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `files` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int DEFAULT NULL,
  `file_url` varchar(255) NOT NULL,
  `file_type` enum('DOCUMENT','IMAGE','VIDEO') NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `reply_comment_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_files_post` (`post_id`),
  KEY `FKki6sgbw482hx0qfmfx8gjqblj` (`reply_comment_id`),
  CONSTRAINT `fk_files_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FKki6sgbw482hx0qfmfx8gjqblj` FOREIGN KEY (`reply_comment_id`) REFERENCES `reply_comments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `program_modules` (
  `program_id` int NOT NULL,
  `module_id` int NOT NULL,
  `position` int DEFAULT NULL,
  PRIMARY KEY (`program_id`,`module_id`),
  KEY `fk_pm_module` (`module_id`),
  CONSTRAINT `fk_pm_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_pm_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `class_module` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` INT NOT NULL,
  `module_id` INT NOT NULL,
  `schedule_type` ENUM('fixed','flexible','online','offline') DEFAULT 'fixed',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_cm_class` (`class_id`),
  KEY `fk_cm_module` (`module_id`),
  CONSTRAINT `fk_cm_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cm_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `class_module_teacher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_module_id` BIGINT NOT NULL,
  `user_id` INT NOT NULL,
  `role` ENUM('MAIN','ASSISTANT','MENTOR') DEFAULT 'MAIN',
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_cmt_class_module` (`class_module_id`),
  KEY `fk_cmt_user` (`user_id`),
  CONSTRAINT `fk_cmt_class_module` FOREIGN KEY (`class_module_id`) REFERENCES `class_module` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cmt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `class_module_schedule_teacher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_module_schedule_id` BIGINT NOT NULL,
  `user_id` INT NOT NULL,
  `role` ENUM('LECTURER','SUPPORTER','OBSERVER') DEFAULT 'LECTURER',
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_cmst_schedule` (`class_module_schedule_id`),
  KEY `fk_cmst_user` (`user_id`),
  CONSTRAINT `fk_cmst_schedule` FOREIGN KEY (`class_module_schedule_id`) REFERENCES `class_module_schedules` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cmst_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `attendance_sessions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `schedule_id` BIGINT NOT NULL, 
  `opened_by` INT NOT NULL,      
  `opened_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `closed_at` DATETIME NULL,
  `is_late` TINYINT(1) DEFAULT 0,
  `late_threshold_minutes` INT DEFAULT 15,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_session_per_schedule` (`schedule_id`),
  KEY `fk_as_schedule` (`schedule_id`),
  KEY `fk_as_teacher` (`opened_by`),
  CONSTRAINT `fk_as_schedule` FOREIGN KEY (`schedule_id`) 
    REFERENCES `class_module_schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_as_teacher` FOREIGN KEY (`opened_by`) 
    REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `attendance_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `schedule_id` BIGINT NOT NULL,
  `student_id` INT NOT NULL,
  `status` ENUM('present', 'absent', 'late', 'excused') DEFAULT 'absent',
  `marked_by` INT NOT NULL,     
  `marked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `notes` TEXT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_attendance` (`schedule_id`, `student_id`),
  KEY `fk_att_schedule` (`schedule_id`),
  KEY `fk_att_student` (`student_id`),
  KEY `fk_att_marker` (`marked_by`),
  CONSTRAINT `fk_att_schedule` FOREIGN KEY (`schedule_id`) 
    REFERENCES `class_module_schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_att_student` FOREIGN KEY (`student_id`) 
    REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_att_marker` FOREIGN KEY (`marked_by`) 
    REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_module_id BIGINT NOT NULL,
    student_id INT NOT NULL,
    theory_score DECIMAL(5, 2) NULL, 
    practice_score DECIMAL(5, 2) NULL, 
    entry_date DATE NULL, 
    entered_by_user_id INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    UNIQUE KEY uk_student_module (student_id, class_module_id),
    FOREIGN KEY (class_module_id) REFERENCES class_module(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entered_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=INNODB;

CREATE TABLE grade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade_id BIGINT NOT NULL, 
    component_changed VARCHAR(100) NOT NULL,
    old_score DECIMAL(5, 2) NULL,
    new_score DECIMAL(5, 2) NULL,
    changed_by_user_id INT NOT NULL,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

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

CREATE TABLE IF NOT EXISTS `student_forms` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` INT NOT NULL,
  `student_id` BIGINT NOT NULL COMMENT 'Học viên làm đơn',
  `class_id` BIGINT NOT NULL COMMENT 'Lớp hiện tại',
  `created_by_user_id` BIGINT NOT NULL COMMENT 'Người lập đơn (Giáo vụ/Admin)',

  `reason` TEXT,
  `module_id` BIGINT,
  `module_session_id` BIGINT,
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


CREATE TABLE gamification_point_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,         
    description TEXT,                   
    max_points INT DEFAULT 0,         
    image_url VARCHAR(255),           
    is_active BOOLEAN DEFAULT TRUE,     
    show_on_dashboard BOOLEAN DEFAULT TRUE, 
    created_by INT,                    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE gamification_behavior_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,        
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gamification_behaviors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,             
    name VARCHAR(255) UNIQUE NOT NULL,        

    frequency_type ENUM('UNLIMITED', 'DAILY', 'WEEKLY', 'MONTHLY', 'ONCE') DEFAULT 'UNLIMITED',
    max_times_per_frequency INT DEFAULT 1, 
    
    -- CÁC TRƯỜNG CŨ (DEPRECATED - Giữ lại để backward compatibility)
    -- Khuyến nghị: Sử dụng bảng behavior_point_types thay vì các trường này
    -- Các trường này vẫn hoạt động nhưng sẽ bị thay thế bởi behavior_point_types
    point_diligence INT DEFAULT 0,      -- DEPRECATED: Dùng behavior_point_types thay thế
    point_competence INT DEFAULT 0,     -- DEPRECATED: Dùng behavior_point_types thay thế
    point_experience INT DEFAULT 0,     -- DEPRECATED: Dùng behavior_point_types thay thế
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES gamification_behavior_groups(id)
);

CREATE TABLE behavior_point_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    behavior_id INT NOT NULL,
    point_type_id INT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    notification_template_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (behavior_id) REFERENCES gamification_behaviors(id) ON DELETE CASCADE,
    FOREIGN KEY (point_type_id) REFERENCES gamification_point_types(id),
    FOREIGN KEY (notification_template_id) REFERENCES notification_templates(id),
    UNIQUE KEY unique_behavior_point_type (behavior_id, point_type_id)
);

CREATE TABLE gamification_achievements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,        
    image_url VARCHAR(255),             
    created_by INT,                     
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gamification_achievement_levels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    achievement_id INT NOT NULL,        
    level_name VARCHAR(100) NOT NULL,   

    required_point_type_id INT,         
                                       
    min_points_required INT DEFAULT 0,  
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES gamification_achievements(id)

);

CREATE TABLE user_point_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,              
    behavior_id INT,                  

    points_diligence_earned INT DEFAULT 0,
    points_competence_earned INT DEFAULT 0,
    points_experience_earned INT DEFAULT 0,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (behavior_id) REFERENCES gamification_behaviors(id)
);

CREATE TABLE user_gamification_stats (
    user_id INT PRIMARY KEY,
    total_diligence INT DEFAULT 0,      
    total_competence INT DEFAULT 0,     
    total_experience INT DEFAULT 0,     
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    achievement_level_id INT NOT NULL, 
    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_displayed BOOLEAN DEFAULT FALSE, 
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (achievement_level_id) REFERENCES gamification_achievement_levels(id)
);