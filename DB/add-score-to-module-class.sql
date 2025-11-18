DROP TABLE IF EXISTS grade_history;
DROP TABLE IF EXISTS grades;

CREATE TABLE grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_module_id BIGINT NOT NULL,
    student_id INT NOT NULL,

    -- CỘT ĐIỂM MỚI (theo yêu cầu 1)
    theory_score DECIMAL(5, 2) NULL, -- Điểm lý thuyết
    practice_score DECIMAL(5, 2) NULL, -- Điểm thực hành

    -- CỘT MỚI (theo yêu cầu 2)
    entry_date DATE NULL, -- 'Chọn ngày' từ UI

    entered_by_user_id INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- (Tùy chọn: Thêm cột status cho Xóa Mềm)
   status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',

    -- Đảm bảo mỗi sinh viên chỉ có 1 hàng điểm cho mỗi môn
    UNIQUE KEY uk_student_module (student_id, class_module_id),

    FOREIGN KEY (class_module_id) REFERENCES class_module(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entered_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=INNODB;

CREATE TABLE grade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade_id BIGINT NOT NULL, -- Liên kết với bảng 'grades' mới

    -- CỘT MỚI: Cho biết cột nào đã thay đổi
    component_changed VARCHAR(100) NOT NULL, -- Ví dụ: "theory_score", "practice_score"

    old_score DECIMAL(5, 2) NULL,
    new_score DECIMAL(5, 2) NULL,
    changed_by_user_id INT NOT NULL,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE notifications
MODIFY COLUMN notification_type ENUM(
    'POST_REACTION', 'POST_COMMENT', 'COMMENT_REACTION', 'COMMENT_REPLY',
    'REPLY_REACTION', 'USER_FOLLOW', 'POST_MENTION', 'COMMENT_MENTION',
    'SYSTEM_ANNOUNCEMENT',
    'GRADE_NEW',
    'GRADE_UPDATED'
) NOT NULL;

INSERT INTO permissions (id, name) VALUES
    (32, 'grade:read_all'),
    (33, 'grade:read_detail'),
    (34, 'grade:update'),
    (35, 'grade:create')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

INSERT INTO role_permissions (role_id, permission_id) VALUES
     (1, 32),
     (3, 32),
     (2, 33),
     (3, 33),
     (1, 34),
     (2, 34),
     (1, 35),
     (2, 35)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
