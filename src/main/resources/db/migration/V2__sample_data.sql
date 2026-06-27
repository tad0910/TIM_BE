
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
(28, 'program:delete'),
(29,'schedule:create'),
(30,'schedule:update'),
(31,'schedule:delete'),
(32, 'grade:read_all'),
(33, 'grade:read_detail'),
(34, 'grade:update'),
(35, 'grade:create'),
(36, 'attendance:open'),
(37, 'attendance:read_all'),
(38, 'grade:delete');

INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
  (1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),
  (1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,22),
  (1,23),(1,24),(1,25),(1,26),(1,27),(1,28), (1,21),
  (1,29),(1,30),(1,31),(1,32),(1,33),(1,34),(1,35),
  (1,36),(1,37),(1,38),

  (2,1),(2,3),(2,5),(2,33),(2,34),(2,35),(2,37),(2, 38),

  (3,1),(3,3),(3,5),(3,6),(3,8),(3,9),(3,19),(3,13),(3,19),(3,22),(3,32),(3,33);
  

INSERT IGNORE INTO users (deleted, job_interest_enabled, id, username, firstname, lastname, password, email)
VALUES
  (0, 0, 1, 'admin', 'Alice', 'Nguyen', '123456', 'admin@example.com'),
  (0, 0, 2, 'teacher_hoa', 'Hoa', 'Tran', '123456', 'hoa.teacher@example.com'),
  (0, 0, 3, 'teacher_long', 'Long', 'Le', '123456', 'long.teacher@example.com'),
  (0, 0, 4, 'student_lan', 'Lan', 'Pham', '123456', 'lan.student@example.com'),
  (0, 0, 5, 'student_bao', 'Bao', 'Nguyen', '123456', 'bao.student@example.com'),
  (0, 0, 6, 'student_khanh', 'Khanh', 'Vu', '123456', 'khanh.student@example.com'),
  (0, 0, 7, 'student_hieu', 'Hieu', 'Dang', '123456', 'hieu.student@example.com'),
  (0, 0, 8, 'student_trang', 'Trang', 'Hoang', '123456', 'trang.student@example.com');

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (1,1),(2,2),(3,2),(4,3),(5,3),(6,3),(7,3),(8,3);

INSERT IGNORE INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (2, '/uploads/teacher_hoa.jpg', 'Ảnh đại diện giáo viên Hoa'),
  (3, '/uploads/teacher_long.jpg', 'Ảnh đại diện giáo viên Long'),
  (4, '/uploads/student_lan.jpg', 'Ảnh đại diện học viên Lan'),
  (5, '/uploads/student_bao.jpg', 'Ảnh đại diện học viên Bảo');

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

INSERT IGNORE INTO classes (jobs_enabled, id, name, description, program_id) VALUES
  (0, 1, 'Fullstack K2025', 'Lớp học fullstack đầu tiên năm 2025', 1),
  (0, 2, 'AI Fundamental 2025', 'Lớp học AI cơ bản cho người mới bắt đầu', 2);

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

INSERT IGNORE INTO notifications (is_read, created_at, receiver_id, sender_id, notification_type, target_type, target_id, title, content)
VALUES
  (0, NOW(), 2, 4, 'POST_COMMENT', 'POST', 1, 'Lan đã bình luận', 'Lan đã bình luận bài đăng của bạn'),
  (0, NOW(), 4, 2, 'SYSTEM_ANNOUNCEMENT', 'SYSTEM', NULL, 'Thông báo lớp học', 'Buổi học tiếp theo sẽ vào thứ 6.');

INSERT IGNORE INTO ranking (nguoi_dung_id, total_experience_score, classes_id, courses_id)
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




INSERT IGNORE INTO users (deleted, job_interest_enabled, id, username, firstname, lastname, password, email) VALUES
  (0, 0, 9,  'teacher_minh',    'Minh',     'Nguyen',   '123456', 'minh.teacher@example.com'),
  (0, 0, 10, 'teacher_thao',    'Thảo',     'Phạm',     '123456', 'thao.teacher@example.com'),
  (0, 0, 11, 'student_tuan',    'Tuấn',     'Lê',       '123456', 'tuan.student@example.com'),
  (0, 0, 12, 'student_ngoc',    'Ngọc',     'Trần',     '123456', 'ngoc.student@example.com'),
  (0, 0, 13, 'student_phong',   'Phong',    'Vũ',       '123456', 'phong.student@example.com'),
  (0, 0, 14, 'student_mai',     'Mai',      'Đỗ',       '123456', 'mai.student@example.com'),
  (0, 0, 15, 'student_anh',     'Anh',      'Hoàng',    '123456', 'anh.student@example.com'),
  (0, 0, 16, 'student_khoa',    'Khoa',     'Bùi',      '123456', 'khoa.student@example.com'),
  (0, 0, 17, 'student_vy',      'Vy',       'Lý',       '123456', 'vy.student@example.com'),
  (0, 0, 18, 'student_duy',     'Duy',      'Trịnh',    '123456', 'duy.student@example.com');


INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (9,2), (10,2), 
  (11,3),(12,3),(13,3),(14,3),(15,3),(16,3),(17,3),(18,3); 


INSERT IGNORE INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (9,  '/uploads/teacher_minh.jpg', 'Ảnh đại diện giáo viên Minh'),
  (10, '/uploads/teacher_thao.jpg', 'Ảnh đại diện giáo viên Thảo'),
  (11, '/uploads/student_tuan.jpg', 'Ảnh đại diện học viên Tuấn'),
  (12, '/uploads/student_ngoc.jpg', 'Ảnh đại diện học viên Ngọc'),
  (13, '/uploads/student_phong.jpg','Ảnh đại diện học viên Phong'),
  (14, '/uploads/student_mai.jpg',  'Ảnh đại diện học viên Mai'),
  (15, '/uploads/student_anh.jpg',  'Ảnh đại diện học viên Anh');


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


INSERT IGNORE INTO classes (jobs_enabled, id, name, description, program_id) VALUES
  (0, 3, 'Mobile Dev K2025', 'Lớp học phát triển ứng dụng di động', 4),
  (0, 4, 'Data Science 2025', 'Lớp học phân tích dữ liệu chuyên sâu', 5),
  (0, 5, 'DevOps Pro 2025', 'Lớp học vận hành hệ thống hiện đại', 6);


INSERT IGNORE INTO class_members (lop_id, nguoi_dung_id, vai_tro) VALUES
  (3,9,'giao_vien'),(3,11,'sinh_vien'),(3,12,'sinh_vien'),(3,13,'sinh_vien'),
  (4,10,'giao_vien'),(4,14,'sinh_vien'),(4,15,'sinh_vien'),(4,16,'sinh_vien'),
  (5,9,'giao_vien'),(5,17,'sinh_vien'),(5,18,'sinh_vien');


INSERT IGNORE INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
  (3,11,'Em vừa hoàn thành bài tập Flutter đầu tiên!','open'),
  (4,9,'Hôm nay chúng ta sẽ học về Stateful vs Stateless Widget.','open'),
  (5,14,'Em đang gặp lỗi khi chạy Pandas, ai giúp được không?','open'),
  (6,10,'Buổi học hôm nay sẽ có bài kiểm tra nhỏ về SQL.','open'),
  (7,17,'Docker thật thú vị! Em đã chạy được container đầu tiên.','open'),
  (8,5,'Em muốn chia sẻ tài liệu học Spring Boot miễn phí.','open'),
  (9,12,'Ai muốn lập nhóm học chung Flutter không?','open'),
  (10,18,'Em cần tài liệu về AWS Lambda, có ai chia sẻ không?','open');


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

INSERT IGNORE INTO reactions (id, bai_viet_id, nguoi_dung_id, loai_cam_xuc) VALUES
  (5,3,9,'like'),(6,3,13,'like'),(7,3,12,'love'),
  (8,4,11,'like'),(9,4,14,'wow'),
  (10,5,15,'like'),(11,5,10,'like'),
  (12,7,18,'like'),(13,7,9,'love'),
  (14,8,2,'like'),(15,8,3,'like'),
  (16,9,13,'like'),(17,9,11,'like');

INSERT IGNORE INTO notifications (is_read, created_at, receiver_id, sender_id, notification_type, target_type, target_id, title, content) VALUES
  (0, NOW(), 9, 11, 'POST_COMMENT', 'POST', 3, 'Tuấn đã bình luận', 'Tuấn đã bình luận về bài đăng Flutter của bạn'),
  (0, NOW(), 11, 9, 'SYSTEM_ANNOUNCEMENT', 'CLASS', 3, 'Lịch học mới', 'Buổi học Flutter tuần này chuyển sang thứ 4'),
  (0, NOW(), 10, 14, 'POST_COMMENT', 'POST', 5, 'Mai đã bình luận', 'Mai cần hỗ trợ Pandas'),
  (0, NOW(), 17, 9, 'REACTION', 'POST', 7, 'Minh đã thả tim', 'Giáo viên Minh đã thả tim bài đăng Docker của bạn'),
  (0, NOW(), 12, 11, 'REPLY_COMMENT', 'COMMENT', 9, 'Tuấn trả lời', 'Tuấn đã trả lời bình luận của bạn'),
  (0, NOW(), 18, 17, 'POST_MENTION', 'POST', 10, 'Bạn được nhắc đến', 'Khoa đã gửi tài liệu AWS cho bạn');

INSERT IGNORE INTO ranking (nguoi_dung_id, total_experience_score, classes_id, courses_id) VALUES
  (11,88,3,4),
  (12,92,3,4),
  (13,76,3,4),
  (14,85,4,5),
  (15,90,4,5),
  (16,82,4,5),
  (17,95,5,6),
  (18,89,5,6);

INSERT IGNORE INTO password_reset_requests (user_id, otp_hash, token_type, attempts, used, expires_at) VALUES
  (11, 'xyz789', 'OTP', 1, 0, DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
  (14, 'def456', 'OTP', 0, 0, DATE_ADD(NOW(), INTERVAL 1 HOUR)),
  (17, 'ghi789', 'OTP', 2, 0, DATE_ADD(NOW(), INTERVAL 15 MINUTE));

INSERT IGNORE INTO invalidated_tokens (jti, expiry_date) VALUES
  ('invalid-token-456', DATE_ADD(NOW(), INTERVAL 1 DAY)),
  ('old-session-789', DATE_ADD(NOW(), INTERVAL 3 DAY)),
  ('logout-all-101112', DATE_ADD(NOW(), INTERVAL 7 DAY));



INSERT IGNORE INTO users (deleted, job_interest_enabled, id, username, firstname, lastname, password, email) VALUES
  (0, 0, 19, 'student_linh',   'Linh',    'Nguyễn', '123456', 'linh.student@example.com'),
  (0, 0, 20, 'student_hao',    'Hào',     'Trần',   '123456', 'hao.student@example.com'),
  (0, 0, 21, 'teacher_kien',   'Kiên',    'Lê',     '123456', 'kien.teacher@example.com'),
  (0, 0, 22, 'student_thuy',   'Thủy',    'Phạm',   '123456', 'thuy.student@example.com'),
  (0, 0, 23, 'student_nam',    'Nam',     'Vũ',     '123456', 'nam.student@example.com'),
  (0, 0, 24, 'teacher_uyen',   'Uyên',    'Hoàng',  '123456', 'uyen.teacher@example.com'),
  (0, 0, 25, 'student_phuc',   'Phúc',    'Đặng',   '123456', 'phuc.student@example.com'),
  (0, 0, 26, 'student_my',     'Mỹ',      'Bùi',    '123456', 'my.student@example.com'),
  (0, 0, 27, 'student_quang',  'Quang',   'Lý',     '123456', 'quang.student@example.com'),
  (0, 0, 28, 'teacher_binh',   'Bình',    'Trương', '123456', 'binh.teacher@example.com'),
  (0, 0, 29, 'student_chau',   'Châu',    'Đỗ',     '123456', 'chau.student@example.com'),
  (0, 0, 30, 'student_hoang',  'Hoàng',   'Phan',   '123456', 'hoang.student@example.com');

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (19,3),(20,3),(21,2),(22,3),(23,3),(24,2),(25,3),(26,3),(27,3),(28,2),(29,3),(30,3);

INSERT IGNORE INTO user_images (nguoi_dung_id, url_anh, mo_ta) VALUES
  (19,'/uploads/student_linh.jpg','Linh'),(20,'/uploads/student_hao.jpg','Hào'),
  (21,'/uploads/teacher_kien.jpg','Kiên'),(22,'/uploads/student_thuy.jpg','Thủy'),
  (23,'/uploads/student_nam.jpg','Nam'),(24,'/uploads/teacher_uyen.jpg','Uyên'),
  (25,'/uploads/student_phuc.jpg','Phúc'),(26,'/uploads/student_my.jpg','Mỹ'),
  (27,'/uploads/student_quang.jpg','Quang'),(28,'/uploads/teacher_binh.jpg','Bình');


INSERT IGNORE INTO programs (id, name, description) VALUES
  (7,'Frontend React Master','Thành thạo React, Redux, Next.js'),
  (8,'Backend Node.js Pro','Xây dựng API mạnh mẽ với NestJS'),
  (9,'UI/UX Design','Thiết kế giao diện người dùng chuyên nghiệp'),
  (10,'Cyber Security Basics','Bảo mật ứng dụng web');


INSERT IGNORE INTO modules (id, name, description) VALUES
  (11,'React Hooks & Context','Quản lý state hiện đại'),
  (12,'Next.js','Server-side rendering với React'),
  (13,'NestJS','Backend TypeScript mạnh mẽ'),
  (14,'MongoDB','Cơ sở dữ liệu NoSQL'),
  (15,'Figma','Thiết kế UI/UX chuyên nghiệp'),
  (16,'Firebase','Backend-as-a-Service'),
  (17,'GraphQL','API query language'),
  (18,'TypeScript','JavaScript typed'),
  (19,'Web Security','OWASP Top 10'),
  (20,'Testing (Jest + Cypress)','Đảm bảo chất lượng code');

INSERT IGNORE INTO program_modules (program_id, module_id, position) VALUES
  (7,11,1),(7,12,2),(7,18,3),
  (8,13,1),(8,14,2),(8,17,3),
  (9,15,1),(9,20,2),
  (10,19,1),(10,16,2);

INSERT IGNORE INTO classes (jobs_enabled, id, name, description, program_id) VALUES
  (0, 6,'React Master 2025','Lớp React nâng cao',7),
  (0, 7,'Node.js Pro 2025','Lớp backend Node chuyên sâu',8),
  (0, 8,'UI/UX Design K2025','Lớp thiết kế giao diện',9),
  (0, 9,'CyberSec 2025','Lớp bảo mật cơ bản',10),
  (0, 10,'Fullstack Elite 2025','Lớp fullstack kết hợp React + NestJS',1);

INSERT IGNORE INTO class_members (lop_id, nguoi_dung_id, vai_tro) VALUES
  (6,21,'giao_vien'),(6,19,'sinh_vien'),(6,20,'sinh_vien'),(6,22,'sinh_vien'),(6,23,'sinh_vien'),
  (7,24,'giao_vien'),(7,25,'sinh_vien'),(7,26,'sinh_vien'),(7,27,'sinh_vien'),
  (8,28,'giao_vien'),(8,29,'sinh_vien'),(8,30,'sinh_vien'),
  (9,21,'giao_vien'),(9,19,'sinh_vien'),(9,25,'sinh_vien'),
  (10,2,'giao_vien'),(10,4,'sinh_vien'),(10,11,'sinh_vien'),(10,14,'sinh_vien');

INSERT IGNORE INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu, link_url, link_title, link_description, link_image_url) VALUES
  (11,19,'Học React Hooks khó quá mọi người ơi!','open','https://react.dev','React Official','Tài liệu chính thức React',NULL),
  (12,21,'Hôm nay học về Custom Hooks','open',NULL,NULL,NULL,NULL),
  (13,25,'Ai có tài liệu NestJS tiếng Việt không?','open',NULL,NULL,NULL,NULL),
  (14,28,'Khóa UI/UX bắt đầu nhận đăng ký!','open','https://figma.com','Figma','Công cụ thiết kế miễn phí',NULL),
  (15,4,'Chia sẻ kinh nghiệm phỏng vấn Fullstack','open',NULL,NULL,NULL,NULL),
  (16,11,'Em vừa deploy app Flutter lên TestFlight!','open',NULL,NULL,NULL,NULL),
  (17,14,'Pandas + Matplotlib = tuyệt vời','open',NULL,NULL,NULL,NULL),
  (18,17,'Docker compose giúp cuộc đời dễ thở hơn','open',NULL,NULL,NULL,NULL),
  (19,20,'Next.js 14 ra mắt rồi, mọi người thử chưa?','open',NULL,NULL,NULL,NULL),
  (20,24,'Buổi học GraphQL tuần này sẽ rất thú vị','open',NULL,NULL,NULL,NULL),
  (21,5,'Tài liệu Spring Security miễn phí đây','open','https://github.com/spring-security','Spring Security','Bảo mật Spring Boot',NULL),
  (22,30,'Em cần mentor 1-1 về React','friends',NULL,NULL,NULL,NULL),
  (23,9,'Thông báo lịch thi giữa kỳ','open',NULL,NULL,NULL,NULL),
  (24,2,'Chúc mừng lớp Fullstack K2025 hoàn thành 50% khóa học!','open',NULL,NULL,NULL,NULL),
  (25,18,'AWS miễn phí 12 tháng cho sinh viên','open','https://aws.amazon.com/education','AWS Educate','Ưu đãi sinh viên',NULL);

INSERT IGNORE INTO class_module (class_id, module_id, schedule_type) VALUES
  (1,1,'fixed'),(1,2,'fixed'),(1,3,'fixed'),
  (2,2,'online'),(2,4,'online'),
  (3,5,'fixed'),(3,6,'flexible'),
  (4,7,'offline'),(4,8,'offline'),
  (5,9,'online'),(5,10,'online'),
  (6,11,'fixed'),(6,12,'fixed'),
  (7,13,'online'),(7,14,'online'),
  (8,15,'offline'),
  (9,19,'online'),
  (10,1,'fixed'),(10,3,'fixed');

INSERT IGNORE INTO class_module_schedules (class_id, module_id, start_date, end_date, status, instructor_id, module_session_id) VALUES
  (1,1,'2025-01-10 08:00:00','2025-01-10 12:00:00','completed',2,1),
  (1,1,'2025-01-17 whether 08:00:00','2025-01-17 12:00:00','completed',2,2),
  (1,2,'2025-02-01 13:00:00','2025-02-01 17:00:00','ongoing',2,NULL),
  (3,5,'2025-03-01 09:00:00','2025-03-01 12:00:00','planned',9,NULL),
  (4,7,'2025-02-15 14:00:00','2025-02-15 17:00:00','ongoing',10,NULL),
  (6,11,'2025-03-10 08:00:00','2025-03-10 12:00:00','planned',21,NULL),
  (7,13,'2025-04-01 18:00:00','2025-04-01 21:00:00','planned',24,NULL),
  (2,4,'2025-03-20 19:00:00','2025-03-20 22:00:00','planned',3,NULL),
  (5,9,'2025-05-01 09:00:00','2025-05-01 12:00:00','planned',9,NULL),
  (10,1,'2025-06-01 08:00:00','2025-06-01 12:00:00','planned',2,NULL);

INSERT IGNORE INTO attendance_sessions (schedule_id, opened_by, opened_at, is_late, late_threshold_minutes) VALUES
  (1,2,'2025-01-10 07:50:00',0,15),
  (2,2,'2025-01-17 07:55:00',1,10),
  (3,2,'2025-02-01 12:55:00',0,15);

INSERT IGNORE INTO attendance_records (schedule_id, student_id, status, marked_by, marked_at) VALUES
  (1,4,'present',2,'2025-01-10 08:05:00'),
  (1,5,'late',2,'2025-01-10 08:20:00'),
  (1,6,'present',2,'2025-01-10 08:00:00'),
  (2,4,'present',2,'2025-01-17 08:10:00'),
  (2,5,'absent',2,'2025-01-17 12:00:00'),
  (3,4,'present',2,'2025-02-01 13:05:00');

INSERT IGNORE INTO grades (class_module_id, student_id, theory_score, practice_score, entry_date, entered_by_user_id) VALUES
  (1,4,8.5,9.0,'2025-01-20',2),
  (1,5,7.0,7.5,'2025-01-20',2),
  (1,6,9.5,9.0,'2025-01-20',2),
  (2,11,8.0,8.5,'2025-03-10',9),
  (3,14,9.0,8.0,'2025-02-20',10),
  (4,19,7.5,9.5,'2025-03-15',21);

INSERT IGNORE INTO grade_history (grade_id, component_changed, old_score, new_score, changed_by_user_id) VALUES
  (1,'practice_score',8.0,9.0,2),
  (2,'theory_score',6.5,7.0,2);

INSERT IGNORE INTO files (post_id, file_url, file_type, file_name, file_size) VALUES
  (8,'/uploads/spring-boot-guide.pdf','DOCUMENT','Spring Boot Guide 2025',5242880),
  (11,'/uploads/react-hooks.png','IMAGE','React Hooks Diagram',245760),
  (15,'/uploads/interview-questions.pdf','DOCUMENT','50 Câu hỏi phỏng vấn Fullstack',1048576),
  (21,'/uploads/spring-security.pdf','DOCUMENT','Spring Security Tutorial',3670016);

INSERT IGNORE INTO invalidated_tokens (jti, expiry_date) VALUES
  ('sess-2025-001','2025-12-01 10:00:00'),
  ('sess-2025-002','2025-11-30 15:30:00'),
  ('sess-2025-003','2025-11-25 09:00:00');

INSERT IGNORE INTO password_reset_requests (user_id, otp_hash, token_type, attempts, used, expires_at) VALUES
  (19,'otp-789abc','OTP',0,0,DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
  (25,'otp-xyz456','OTP',1,0,DATE_ADD(NOW(), INTERVAL 20 MINUTE)),
  (5,'link-reset-123','LINK',0,0,DATE_ADD(NOW(), INTERVAL 1 HOUR));