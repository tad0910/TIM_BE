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
  `notification_type` enum('POST_REACTION','POST_COMMENT','COMMENT_REACTION','COMMENT_REPLY','REPLY_REACTION','USER_FOLLOW','POST_MENTION','COMMENT_MENTION','SYSTEM_ANNOUNCEMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
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
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL,
  `module_id` int NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('planned','ongoing','completed') NOT NULL DEFAULT 'planned',
  `instructor_id` int DEFAULT NULL,
  `notes` text,
  PRIMARY KEY (`id`),
  KEY `fk_cms_class` (`class_id`),
  KEY `fk_cms_module` (`module_id`),
  KEY `fk_cms_instructor` (`instructor_id`),
  CONSTRAINT `fk_cms_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cms_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_cms_module` FOREIGN KEY (`module_id`) REFERENCES `modules` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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


