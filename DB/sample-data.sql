-- =====================================
-- 1. Roles & Permissions
-- =====================================
INSERT INTO roles (id, name) VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_GIAO_VIEN'),
  (3, 'ROLE_STUDENT'),
  (4, 'ROLE_USER'),

INSERT INTO permissions (id, name) VALUES
(1, 'user:read_all'),
(2, 'user:update_all'),
(3, 'user:create'),
(4, 'user:delete'),
(5, 'post:create'),
(6, 'post:delete_all'),
(7, 'user:logout_all'),
(8, 'reaction:create'),
(9, 'reaction:delete'),
(10, 'class:create'),
(11, 'class:update_all'),
(12, 'class:delete_all'),
(13, 'comment:create'),
(14, 'comment:update_all'),
(15, 'comment:delete_all'),
(16, 'course:create'),
(17, 'course:update'),
(18, 'course:delete'),
(19, 'notification:cleanup'),
(20, 'notification:create_manual'),
(21, 'post:update_all'),
(22, 'class:read_all'),
(23, 'module:create'),
(24, 'module:update'),
(25, 'module:delete'),
(26, 'program:create'),
(27, 'program:update'),
(28, 'program:delete');

INSERT INTO role_permissions (role_id, permission_id) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
  (1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),
  (1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,22),
  (1,23),(1,24),(1,25),(1,26),(1,27),(1,28), (1,21);
  (2,1),(2,3),(2,5),
  (3,3),(3,5);

-- =====================================
-- 2. Users & Roles
-- =====================================
INSERT INTO users (id, username, firstname, lastname, password, email, vai_tro)
VALUES
  (1, 'admin', 'Alice', 'Nguyen', '123456', 'admin@example.com', 'admin'),
  (2, 'teacher_hoa', 'Hoa', 'Tran', '123456', 'hoa.teacher@example.com', 'giao_vien'),
  (3, 'teacher_long', 'Long', 'Le', '123456', 'long.teacher@example.com', 'giao_vien'),
  (4, 'student_lan', 'Lan', 'Pham', '123456', 'lan.student@example.com', 'sinh_vien'),
  (5, 'student_bao', 'Bao', 'Nguyen', '123456', 'bao.student@example.com', 'sinh_vien'),
  (6, 'student_khanh', 'Khanh', 'Vu', '123456', 'khanh.student@example.com', 'sinh_vien'),
  (7, 'student_hieu', 'Hieu', 'Dang', '123456', 'hieu.student@example.com', 'sinh_vien'),
  (8, 'student_trang', 'Trang', 'Hoang', '123456', 'trang.student@example.com', 'sinh_vien');

INSERT INTO user_roles (user_id, role_id) VALUES
  (1,1),(2,2),(3,2),(4,3),(5,3),(6,3),(7,3),(8,3);

INSERT INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (2, '/uploads/teacher_hoa.jpg', 'Ảnh đại diện giáo viên Hoa'),
  (3, '/uploads/teacher_long.jpg', 'Ảnh đại diện giáo viên Long'),
  (4, '/uploads/student_lan.jpg', 'Ảnh đại diện học viên Lan'),
  (5, '/uploads/student_bao.jpg', 'Ảnh đại diện học viên Bảo');

-- =====================================
-- 3. Programs & Modules
-- =====================================
INSERT INTO programs (id, name, description) VALUES
  (1, 'Fullstack Web Development', 'Khóa học lập trình web toàn diện'),
  (2, 'Python AI Fundamentals', 'Khóa học AI và Machine Learning cơ bản'),
  (3, 'Java Backend Mastery', 'Khóa học phát triển web backend với Spring Boot');

INSERT INTO modules (id, name, description) VALUES
  (1, 'HTML & CSS', 'Nắm vững nền tảng giao diện web'),
  (2, 'JavaScript', 'Lập trình front-end hiện đại'),
  (3, 'Spring Boot', 'Phát triển API backend bằng Java'),
  (4, 'Machine Learning', 'Giới thiệu các thuật toán AI cơ bản');

INSERT INTO program_modules (program_id, module_id, position) VALUES
  (1,1,1),(1,2,2),(1,3,3),
  (2,2,1),(2,4,2),
  (3,3,1),(3,4,2);

-- =====================================
-- 4. Classes & Members
-- =====================================
INSERT INTO classes (id, name, description, program_id) VALUES
  (1, 'Fullstack K2025', 'Lớp học fullstack đầu tiên năm 2025', 1),
  (2, 'AI Fundamental 2025', 'Lớp học AI cơ bản cho người mới bắt đầu', 2);

INSERT INTO class_members (lop_id, nguoi_dung_id, vai_tro) VALUES
  (1,2,'giao_vien'),(1,4,'sinh_vien'),(1,5,'sinh_vien'),(1,6,'sinh_vien'),
  (2,3,'giao_vien'),(2,7,'sinh_vien'),(2,8,'sinh_vien');

-- =====================================
-- 5. Posts, Comments, Replies, Reactions
-- =====================================
INSERT INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
  (1,4,'Em vừa hoàn thành module HTML & CSS!','open'),
  (2,2,'Chúc mừng các em đã hoàn thành buổi đầu tiên!','open');

INSERT INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung) VALUES
  (1,1,2,'Rất tốt, Lan! Cố gắng phát huy nhé!'),
  (2,1,5,'Cảm ơn cô ạ! Em sẽ cố gắng!'),
  (3,2,6,'Buổi học rất bổ ích, cảm ơn cô.');

INSERT INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung) VALUES
  (1,1,4,'Em cảm ơn cô nhiều ạ!'),
  (2,3,2,'Rất vui khi nghe điều đó!');

INSERT INTO reactions (id, bai_viet_id, nguoi_dung_id, loai_cam_xuc) VALUES
  (1,1,5,'like'),
  (2,1,2,'love'),
  (3,2,4,'wow'),
  (4,2,6,'like');

-- =====================================
-- 6. Notifications
-- =====================================
INSERT INTO notifications (receiver_id, sender_id, notification_type, target_type, target_id, title, content)
VALUES
  (2,4,'POST_COMMENT','POST',1,'Lan đã bình luận','Lan đã bình luận bài đăng của bạn'),
  (4,2,'SYSTEM_ANNOUNCEMENT','SYSTEM',NULL,'Thông báo lớp học','Buổi học tiếp theo sẽ vào thứ 6.');

-- =====================================
-- 7. Ranking (optional)
-- =====================================
INSERT INTO ranking (nguoi_dung_id, diem_tong_hop, classes_id, program_id)
VALUES
  (4,85,1,1),
  (5,75,1,1),
  (6,90,1,1),
  (7,80,2,2),
  (8,88,2,2);

-- =====================================
-- 8. Password Reset & Token (demo)
-- =====================================
INSERT INTO password_reset_requests (user_id, otp_hash, token_type, attempts, used, expires_at)
VALUES
  (4, 'abc123', 'OTP', 0, 0, DATE_ADD(NOW(), INTERVAL 1 DAY));

INSERT INTO invalidated_tokens (jti, expiry_date)
VALUES ('sample-token-123', DATE_ADD(NOW(), INTERVAL 2 DAY));
