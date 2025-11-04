-- Migration script: Thêm instructor_id vào bảng modules và module_sessions
-- Date: 2024

USE dbtest;

-- Thêm cột instructor_id vào bảng modules
ALTER TABLE `modules`
ADD COLUMN `instructor_id` int DEFAULT NULL AFTER `description`,
ADD KEY `fk_modules_instructor` (`instructor_id`),
ADD CONSTRAINT `fk_modules_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- Thêm cột instructor_id vào bảng module_sessions
ALTER TABLE `module_sessions`
ADD COLUMN `instructor_id` int DEFAULT NULL AFTER `status`,
ADD KEY `fk_ms_instructor` (`instructor_id`),
ADD CONSTRAINT `fk_ms_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
