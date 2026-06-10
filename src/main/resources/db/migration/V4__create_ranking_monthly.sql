-- Tạo bảng ranking_monthly để lưu snapshot xếp hạng theo tháng
CREATE TABLE IF NOT EXISTS `ranking_monthly` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    month_year VARCHAR(7) NOT NULL, -- Format: 'YYYY-MM' (ví dụ: '2025-11')
    total_diligence_score INT DEFAULT 0,
    total_competence_score INT DEFAULT 0,
    total_experience_score INT DEFAULT 0,
    rank_position INT, -- Vị trí xếp hạng trong tháng (tính từ total_experience_score)
    class_id INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, month_year, class_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    INDEX idx_month_year (month_year),
    INDEX idx_total_experience_score (total_experience_score DESC),
    INDEX idx_total_competence_score (total_competence_score DESC),
    INDEX idx_rank_position (rank_position ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

