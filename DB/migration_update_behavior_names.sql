
-- 1. Điểm danh đúng giờ
UPDATE gamification_behaviors 
SET name = 'Điểm danh đúng giờ' 
WHERE name = 'ATTEND_ON_TIME'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Điểm danh đúng giờ');

-- 2. Đạt điểm cao (>80%)
UPDATE gamification_behaviors 
SET name = 'Đạt điểm cao (>80%)' 
WHERE name = 'HIGH_POINT_1'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Đạt điểm cao (>80%)');

-- 3. Đạt điểm xuất sắc (>95%)
UPDATE gamification_behaviors 
SET name = 'Đạt điểm xuất sắc (>95%)' 
WHERE name = 'HIGH_POINT_2'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Đạt điểm xuất sắc (>95%)');

-- 4. Đọc tin tức lần đầu
UPDATE gamification_behaviors 
SET name = 'Đọc tin tức lần đầu' 
WHERE name = 'READ_BLOG'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Đọc tin tức lần đầu');

-- 5. Đăng bài viết đầu tiên
UPDATE gamification_behaviors 
SET name = 'Đăng bài viết đầu tiên' 
WHERE name = 'FIRST_POST'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Đăng bài viết đầu tiên');

-- 6. Bài viết được yêu thích (>10 likes)
UPDATE gamification_behaviors 
SET name = 'Bài viết được yêu thích (>10 likes)' 
WHERE name = 'POST''S_LIKE'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Bài viết được yêu thích (>10 likes)');

-- 7. Chia sẻ kiến thức (Bài viết có link)
UPDATE gamification_behaviors 
SET name = 'Chia sẻ kiến thức (Bài viết có link)' 
WHERE name = 'POST_SHARE'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Chia sẻ kiến thức (Bài viết có link)');

-- 8. Giáo viên chấm điểm 10
UPDATE gamification_behaviors 
SET name = 'Giáo viên chấm điểm 10' 
WHERE name = 'GIVING_SCORES'
  AND NOT EXISTS (SELECT 1 FROM gamification_behaviors WHERE name = 'Giáo viên chấm điểm 10');

-- Verify: Kiểm tra kết quả
SELECT id, name, group_id, frequency_type, max_times_per_frequency 
FROM gamification_behaviors 
WHERE name IN (
    'Điểm danh đúng giờ',
    'Đạt điểm cao (>80%)',
    'Đạt điểm xuất sắc (>95%)',
    'Đọc tin tức lần đầu',
    'Đăng bài viết đầu tiên',
    'Bài viết được yêu thích (>10 likes)',
    'Chia sẻ kiến thức (Bài viết có link)',
    'Giáo viên chấm điểm 10'
)
ORDER BY name;

