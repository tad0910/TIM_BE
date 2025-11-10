USE dbtest;
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


-- 2) Tạo bảng class_module_teacher (giáo viên của 1 class_module)
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


-- 3) Tạo bảng class_module_schedule_teacher (giáo viên cho từng buổi trong class_module_schedules)
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


-- 4) Thêm cột class_module_id vào class_module_schedules để liên kết trực tiếp
ALTER TABLE `class_module_schedules`
  ADD COLUMN `class_module_id` BIGINT NULL AFTER `module_id`,
  ADD KEY `fk_cms_class_module` (`class_module_id`),
  ADD CONSTRAINT `fk_cms_class_module` FOREIGN KEY (`class_module_id`) REFERENCES `class_module` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
