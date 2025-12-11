-- Migration: Thêm bảng behavior_point_types để hỗ trợ chọn loại điểm thưởng động cho hành vi
-- Ngày tạo: 2024-01-15

-- Tạo bảng behavior_point_types
CREATE TABLE IF NOT EXISTS behavior_point_types (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo index để tối ưu truy vấn
CREATE INDEX idx_behavior_point_types_behavior_id ON behavior_point_types(behavior_id);
CREATE INDEX idx_behavior_point_types_point_type_id ON behavior_point_types(point_type_id);

