USE dbtest;
INSERT IGNORE INTO roles (id, name) VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_GIAO_VIEN'),
  (3, 'ROLE_USER');

INSERT IGNORE INTO permissions (id, name) VALUES
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

INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
  (1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),
  (1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,22),
  (1,23),(1,24),(1,25),(1,26),(1,27),(1,28), (1,21),
  (2,1),(2,3),(2,5),
  (3,3),(3,5);

INSERT IGNORE INTO users (id, username, firstname, lastname, password, email, vai_tro)
VALUES
  (1, 'admin', 'Alice', 'Nguyen', '123456', 'admin@example.com', 'admin'),
  (2, 'teacher_hoa', 'Hoa', 'Tran', '123456', 'hoa.teacher@example.com', 'giao_vien'),
  (3, 'teacher_long', 'Long', 'Le', '123456', 'long.teacher@example.com', 'giao_vien'),
  (4, 'student_lan', 'Lan', 'Pham', '123456', 'lan.student@example.com', 'sinh_vien'),
  (5, 'student_bao', 'Bao', 'Nguyen', '123456', 'bao.student@example.com', 'sinh_vien'),
  (6, 'student_khanh', 'Khanh', 'Vu', '123456', 'khanh.student@example.com', 'sinh_vien'),
  (7, 'student_hieu', 'Hieu', 'Dang', '123456', 'hieu.student@example.com', 'sinh_vien'),
  (8, 'student_trang', 'Trang', 'Hoang', '123456', 'trang.student@example.com', 'sinh_vien');

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (1,1),(2,2),(3,2),(4,3),(5,3),(6,3),(7,3),(8,3);

INSERT IGNORE INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (2, '/uploads/teacher_hoa.jpg', 'Ảnh đại diện giáo viên Hoa'),
  (3, '/uploads/teacher_long.jpg', 'Ảnh đại diện giáo viên Long'),
  (4, '/uploads/student_lan.jpg', 'Ảnh đại diện học viên Lan'),
  (5, '/uploads/student_bao.jpg', 'Ảnh đại diện học viên Bảo');

-- =====================================
-- 3. Programs & Modules
-- =====================================
INSERT IGNORE INTO programs (id, name, description) VALUES
  (1, 'Fullstack Web Development', 'Khóa học lập trình web toàn diện'),
  (2, 'Python AI Fundamentals', 'Khóa học AI và Machine Learning cơ bản'),
  (3, 'Java Backend Mastery', 'Khóa học phát triển web backend với Spring Boot');

INSERT IGNORE INTO modules (id, name, description) VALUES
  (1, 'HTML & CSS', 'Nắm vững nền tảng giao diện web'),
  (2, 'JavaScript', 'Lập trình front-end hiện đại'),
  (3, 'Spring Boot', 'Phát triển API backend bằng Java'),
  (4, 'Machine Learning', 'Giới thiệu các thuật toán AI cơ bản');

INSERT IGNORE INTO program_modules (program_id, module_id, position) VALUES
  (1,1,1),(1,2,2),(1,3,3),
  (2,2,1),(2,4,2),
  (3,3,1),(3,4,2);

INSERT IGNORE INTO classes (id, name, description, program_id) VALUES
  (1, 'Fullstack K2025', 'Lớp học fullstack đầu tiên năm 2025', 1),
  (2, 'AI Fundamental 2025', 'Lớp học AI cơ bản cho người mới bắt đầu', 2);

INSERT IGNORE INTO class_members (lop_id, nguoi_dung_id, vai_tro) VALUES
  (1,2,'giao_vien'),(1,4,'sinh_vien'),(1,5,'sinh_vien'),(1,6,'sinh_vien'),
  (2,3,'giao_vien'),(2,7,'sinh_vien'),(2,8,'sinh_vien');

INSERT IGNORE INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
  (1,4,'Em vừa hoàn thành module HTML & CSS!','open'),
  (2,2,'Chúc mừng các em đã hoàn thành buổi đầu tiên!','open');

INSERT IGNORE INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung) VALUES
  (1,1,2,'Rất tốt, Lan! Cố gắng phát huy nhé!'),
  (2,1,5,'Cảm ơn cô ạ! Em sẽ cố gắng!'),
  (3,2,6,'Buổi học rất bổ ích, cảm ơn cô.');

INSERT IGNORE INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung) VALUES
  (1,1,4,'Em cảm ơn cô nhiều ạ!'),
  (2,3,2,'Rất vui khi nghe điều đó!');

INSERT IGNORE INTO reactions (id, bai_viet_id, nguoi_dung_id, loai_cam_xuc) VALUES
  (1,1,5,'like'),
  (2,1,2,'love'),
  (3,2,4,'wow'),
  (4,2,6,'like');

INSERT IGNORE INTO notifications (receiver_id, sender_id, notification_type, target_type, target_id, title, content)
VALUES
  (2,4,'POST_COMMENT','POST',1,'Lan đã bình luận','Lan đã bình luận bài đăng của bạn'),
  (4,2,'SYSTEM_ANNOUNCEMENT','SYSTEM',NULL,'Thông báo lớp học','Buổi học tiếp theo sẽ vào thứ 6.');

INSERT IGNORE INTO ranking (nguoi_dung_id, diem_tong_hop, classes_id, program_id)
VALUES
  (4,85,1,1),
  (5,75,1,1),
  (6,90,1,1),
  (7,80,2,2),
  (8,88,2,2);


INSERT IGNORE INTO password_reset_requests (user_id, otp_hash, token_type, attempts, used, expires_at)
VALUES
  (4, 'abc123', 'OTP', 0, 0, DATE_ADD(NOW(), INTERVAL 1 DAY));

INSERT IGNORE INTO invalidated_tokens (jti, expiry_date)
VALUES ('sample-token-123', DATE_ADD(NOW(), INTERVAL 2 DAY));

USE dbtest;

-- =====================================
-- 1. Thêm nhiều Users (sinh viên + giáo viên)
-- =====================================
INSERT IGNORE INTO users (id, username, firstname, lastname, password, email, vai_tro) VALUES
  (9,  'teacher_minh',    'Minh',     'Nguyen',   '123456', 'minh.teacher@example.com', 'giao_vien'),
  (10, 'teacher_thao',    'Thảo',     'Phạm',     '123456', 'thao.teacher@example.com', 'giao_vien'),
  (11, 'student_tuan',    'Tuấn',     'Lê',       '123456', 'tuan.student@example.com', 'sinh_vien'),
  (12, 'student_ngoc',    'Ngọc',     'Trần',     '123456', 'ngoc.student@example.com', 'sinh_vien'),
  (13, 'student_phong',   'Phong',    'Vũ',       '123456', 'phong.student@example.com','sinh_vien'),
  (14, 'student_mai',     'Mai',      'Đỗ',       '123456', 'mai.student@example.com',  'sinh_vien'),
  (15, 'student_anh',     'Anh',      'Hoàng',    '123456', 'anh.student@example.com',  'sinh_vien'),
  (16, 'student_khoa',    'Khoa',     'Bùi',      '123456', 'khoa.student@example.com', 'sinh_vien'),
  (17, 'student_vy',      'Vy',       'Lý',       '123456', 'vy.student@example.com',   'sinh_vien'),
  (18, 'student_duy',     'Duy',      'Trịnh',    '123456', 'duy.student@example.com',  'sinh_vien');

-- Gán vai trò (giả sử đã có user_roles cho các user mới)
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (9,2), (10,2),  -- Giáo viên
  (11,3),(12,3),(13,3),(14,3),(15,3),(16,3),(17,3),(18,3); -- Sinh viên

-- =====================================
-- 2. user_images cho các user mới
-- =====================================
INSERT IGNORE INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (9,  '/uploads/teacher_minh.jpg', 'Ảnh đại diện giáo viên Minh'),
  (10, '/uploads/teacher_thao.jpg', 'Ảnh đại diện giáo viên Thảo'),
  (11, '/uploads/student_tuan.jpg', 'Ảnh đại diện học viên Tuấn'),
  (12, '/uploads/student_ngoc.jpg', 'Ảnh đại diện học viên Ngọc'),
  (13, '/uploads/student_phong.jpg','Ảnh đại diện học viên Phong'),
  (14, '/uploads/student_mai.jpg',  'Ảnh đại diện học viên Mai'),
  (15, '/uploads/student_anh.jpg',  'Ảnh đại diện học viên Anh');

-- =====================================
-- 3. Thêm Programs & Modules mới
-- =====================================
INSERT IGNORE INTO programs (id, name, description) VALUES
  (4, 'Mobile App Development', 'Phát triển ứng dụng di động với Flutter'),
  (5, 'Data Science Bootcamp', 'Từ dữ liệu thô đến mô hình dự đoán'),
  (6, 'DevOps & Cloud', 'Triển khai hệ thống trên AWS và Docker');

INSERT IGNORE INTO modules (id, name, description) VALUES
  (5, 'Flutter & Dart', 'Xây dựng ứng dụng đa nền tảng'),
  (6, 'React Native', 'Lập trình mobile với JavaScript'),
  (7, 'Python for Data Science', 'Pandas, NumPy, Matplotlib'),
  (8, 'SQL & Database Design', 'Thiết kế và truy vấn CSDL'),
  (9, 'Docker & Kubernetes', 'Container hóa và Orchestration'),
  (10, 'AWS Cloud Fundamentals', 'Dịch vụ đám mây cơ bản');

INSERT IGNORE INTO program_modules (program_id, module_id, position) VALUES
  (4,5,1),(4,6,2),
  (5,7,1),(5,8,2),
  (6,9,1),(6,10,2);

-- =====================================
-- 4. Thêm Classes mới
-- =====================================
INSERT IGNORE INTO classes (id, name, description, program_id) VALUES
  (3, 'Mobile Dev K2025', 'Lớp học phát triển ứng dụng di động', 4),
  (4, 'Data Science 2025', 'Lớp học phân tích dữ liệu chuyên sâu', 5),
  (5, 'DevOps Pro 2025', 'Lớp học vận hành hệ thống hiện đại', 6);

-- =====================================
-- 5. class_members (gán giáo viên & sinh viên vào lớp)
-- =====================================
INSERT IGNORE INTO class_members (lop_id, nguoi_dung_id, vai_tro) VALUES
  -- Lớp 3: Mobile Dev
  (3,9,'giao_vien'),(3,11,'sinh_vien'),(3,12,'sinh_vien'),(3,13,'sinh_vien'),
  -- Lớp 4: Data Science
  (4,10,'giao_vien'),(4,14,'sinh_vien'),(4,15,'sinh_vien'),(4,16,'sinh_vien'),
  -- Lớp 5: DevOps
  (5,9,'giao_vien'),(5,17,'sinh_vien'),(5,18,'sinh_vien');

-- =====================================
-- 6. Posts mới từ nhiều người
-- =====================================
INSERT IGNORE INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
  (3,11,'Em vừa hoàn thành bài tập Flutter đầu tiên!','open'),
  (4,9,'Hôm nay chúng ta sẽ học về Stateful vs Stateless Widget.','open'),
  (5,14,'Em đang gặp lỗi khi chạy Pandas, ai giúp được không?','open'),
  (6,10,'Buổi học hôm nay sẽ có bài kiểm tra nhỏ về SQL.','open'),
  (7,17,'Docker thật thú vị! Em đã chạy được container đầu tiên.','open'),
  (8,5,'Em muốn chia sẻ tài liệu học Spring Boot miễn phí.','open'),
  (9,12,'Ai muốn lập nhóm học chung Flutter không?','open'),
  (10,18,'Em cần tài liệu về AWS Lambda, có ai chia sẻ không?','open');

-- =====================================
-- 7. Comments & Reply Comments
-- =====================================
INSERT IGNORE INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung) VALUES
  (4,3,9,'Tuyệt vời Tuấn! Gửi code lên để cô xem nhé!'),
  (5,3,12,'Mình cũng đang làm, để mình tag bạn vào group.'),
  (6,5,10,'Gửi lỗi cụ thể và đoạn code nhé, cô sẽ hỗ trợ.'),
  (7,7,9,'Chúc mừng! Bước đầu tiên rất quan trọng.'),
  (8,8,6,'Cảm ơn Bảo, tài liệu rất hữu ích!'),
  (9,9,11,'Mình tham gia! Khi nào họp nhóm?'),
  (10,10,17,'Mình có tài liệu, để mình gửi link Google Drive.');

INSERT IGNORE INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung) VALUES
  (3,4,11,'Dạ em gửi rồi ạ, trong kênh #homework'),
  (4,6,14,'Dạ em gửi rồi cô ơi, cảm ơn cô!'),
  (5,9,12,'Tối nay 8h nhé, trên Discord.'),
  (6,10,18,'Cảm ơn Khoa nhiều!');

-- =====================================
-- 8. Reactions
-- =====================================
INSERT IGNORE INTO reactions (id, bai_viet_id, nguoi_dung_id, loai_cam_xuc) VALUES
  (5,3,9,'like'),(6,3,13,'like'),(7,3,12,'love'),
  (8,4,11,'like'),(9,4,14,'wow'),
  (10,5,15,'like'),(11,5,10,'like'),
  (12,7,18,'like'),(13,7,9,'love'),
  (14,8,2,'like'),(15,8,3,'like'),
  (16,9,13,'like'),(17,9,11,'like');

-- =====================================
-- 9. Notifications
-- =====================================
INSERT IGNORE INTO notifications (receiver_id, sender_id, notification_type, target_type, target_id, title, content) VALUES
  (9,11,'POST_COMMENT','POST',3,'Tuấn đã bình luận','Tuấn đã bình luận về bài đăng Flutter của bạn'),
  (11,9,'SYSTEM_ANNOUNCEMENT','CLASS',3,'Lịch học mới','Buổi học Flutter tuần này chuyển sang thứ 4'),
  (10,14,'POST_COMMENT','POST',5,'Mai đã bình luận','Mai cần hỗ trợ Pandas'),
  (17,9,'REACTION','POST',7,'Minh đã thả tim','Giáo viên Minh đã thả tim bài đăng Docker của bạn'),
  (12,11,'REPLY_COMMENT','COMMENT',9,'Tuấn trả lời','Tuấn đã trả lời bình luận của bạn'),
  (18,17,'POST_MENTION','POST',10,'Bạn được nhắc đến','Khoa đã gửi tài liệu AWS cho bạn');

-- =====================================
-- 10. Ranking (điểm số các lớp)
-- =====================================
INSERT IGNORE INTO ranking (nguoi_dung_id, diem_tong_hop, classes_id, program_id) VALUES
  (11,88,3,4),
  (12,92,3,4),
  (13,76,3,4),
  (14,85,4,5),
  (15,90,4,5),
  (16,82,4,5),
  (17,95,5,6),
  (18,89,5,6);

-- =====================================
-- 11. password_reset_requests (yêu cầu đặt lại mật khẩu)
-- =====================================
INSERT IGNORE INTO password_reset_requests (user_id, otp_hash, token_type, attempts, used, expires_at) VALUES
  (11, 'xyz789', 'OTP', 1, 0, DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
  (14, 'def456', 'OTP', 0, 0, DATE_ADD(NOW(), INTERVAL 1 HOUR)),
  (17, 'ghi789', 'OTP', 2, 0, DATE_ADD(NOW(), INTERVAL 15 MINUTE));

-- =====================================
-- 12. invalidated_tokens (token bị vô hiệu hóa)
-- =====================================
INSERT IGNORE INTO invalidated_tokens (jti, expiry_date) VALUES
  ('invalid-token-456', DATE_ADD(NOW(), INTERVAL 1 DAY)),
  ('old-session-789', DATE_ADD(NOW(), INTERVAL 3 DAY)),
  ('logout-all-101112', DATE_ADD(NOW(), INTERVAL 7 DAY));

  INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
(3,1), (3,2), (3,6), (3,8), (3,9), (3,13), (3,14), (3,15), (3,19), (3,21), (3,22);