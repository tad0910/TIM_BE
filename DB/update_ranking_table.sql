-- Migration: Cập nhật bảng ranking
-- Bỏ cột diem_tong_hop và program_id
-- Thêm các cột total_diligence_score, total_competence_score, total_experience_score
-- (Sử dụng suffix _score để tránh trùng với các cột trong bảng khác)

-- Bước 1: Thêm các cột mới
ALTER TABLE `ranking` 
ADD COLUMN `total_diligence_score` INT DEFAULT 0 AFTER `nguoi_dung_id`,
ADD COLUMN `total_competence_score` INT DEFAULT 0 AFTER `total_diligence_score`,
ADD COLUMN `total_experience_score` INT DEFAULT 0 AFTER `total_competence_score`;

-- Bước 2: Cập nhật dữ liệu từ user_gamification_stats (nếu có)
UPDATE `ranking` r
INNER JOIN `user_gamification_stats` s ON r.nguoi_dung_id = s.user_id
SET 
    r.total_diligence_score = COALESCE(s.total_diligence, 0),
    r.total_competence_score = COALESCE(s.total_competence, 0),
    r.total_experience_score = COALESCE(s.total_experience, 0);

-- Bước 3: Xóa cột diem_tong_hop (nếu tồn tại)
ALTER TABLE `ranking` DROP COLUMN IF EXISTS `diem_tong_hop`;

-- Bước 4: Xóa foreign key và cột program_id (nếu tồn tại)
ALTER TABLE `ranking` DROP FOREIGN KEY IF EXISTS `fk_ranking_program`;
ALTER TABLE `ranking` DROP COLUMN IF EXISTS `program_id`;

-- Bước 5: Thêm index để tối ưu query
ALTER TABLE `ranking` 
ADD INDEX `idx_total_experience_score` (`total_experience_score` DESC),
ADD INDEX `idx_total_competence_score` (`total_competence_score` DESC),
ADD INDEX `idx_total_diligence_score` (`total_diligence_score` DESC);

