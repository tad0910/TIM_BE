CREATE TABLE IF NOT EXISTS `grades` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_module_id` BIGINT NOT NULL,          -- FK to `class_module` (Knows which class & module this grade belongs to)
  `student_id` INT NOT NULL,                -- FK to `users` (Knows which student this grade belongs to)
  `component_name` VARCHAR(255) NOT NULL,     -- Grade component name: "Midterm", "Final", "Assignment 1"...
  `score` DECIMAL(5, 2) NOT NULL,             -- The score (e.g., 8.50)
  `max_score` DECIMAL(5, 2) DEFAULT 10.00,  -- Maximum possible score (usually 10)
  `weight_percent` DECIMAL(5, 4) DEFAULT NULL,    -- The weight (e.g., 0.3 for 30%)
  `entered_by_user_id` INT NULL,              -- FK to `users` (Which teacher/admin entered this)
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_grades_class_module` (`class_module_id`),
  KEY `fk_grades_student` (`student_id`),
  KEY `fk_grades_entered_by` (`entered_by_user_id`),
  CONSTRAINT `fk_grades_class_module` FOREIGN KEY (`class_module_id`) REFERENCES `class_module` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_grades_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_grades_entered_by` FOREIGN KEY (`entered_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  -- Ensures a student has only one "Midterm" score (for example) in one class_module
  UNIQUE KEY `uk_student_class_module_component` (`student_id`, `class_module_id`, `component_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `grade_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `grade_id` BIGINT NOT NULL,               -- FK to the `grades` table
  `old_score` DECIMAL(5, 2) NOT NULL,
  `new_score` DECIMAL(5, 2) NOT NULL,
  `changed_by_user_id` INT NOT NULL,        -- FK to `users` (Who made the change)
  `change_reason` TEXT NULL,                -- Reason (e.g., "Re-evaluation", "Input error")
  `changed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_gh_grade_id` (`grade_id`),
  KEY `fk_gh_changed_by` (`changed_by_user_id`),
  CONSTRAINT `fk_gh_grade_id` FOREIGN KEY (`grade_id`) REFERENCES `grades` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_gh_changed_by` FOREIGN KEY (`changed_by_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE notifications
MODIFY COLUMN notification_type ENUM(
    'POST_REACTION', 'POST_COMMENT', 'COMMENT_REACTION', 'COMMENT_REPLY',
    'REPLY_REACTION', 'USER_FOLLOW', 'POST_MENTION', 'COMMENT_MENTION',
    'SYSTEM_ANNOUNCEMENT',
    'GRADE_NEW',
    'GRADE_UPDATED'
) NOT NULL;

INSERT INTO permissions (id, name) VALUES
    (34, 'grade:read_all'),
    (35, 'grade:read_detail'),
    (36, 'grade:update'),
    (37, 'grade:create')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

INSERT INTO role_permissions (role_id, permission_id) VALUES
     (1, 31),
     (3, 31),
     (2, 32),
     (3, 32),
     (1, 33),
     (2, 33),
     (1, 34),
     (2, 34)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
