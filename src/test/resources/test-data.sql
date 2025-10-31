-- === 1. XÓA DỮ LIỆU CŨ (Tuân thủ thứ tự khóa ngoại) ===

DELETE FROM role_permissions;
DELETE FROM user_roles;

-- Xóa dữ liệu Lịch học và Chương trình mới
DELETE FROM class_module_schedules;
DELETE FROM module_sessions;
DELETE FROM program_modules;
DELETE FROM classes;
DELETE FROM modules;
DELETE FROM programs;


-- Xóa dữ liệu Mạng xã hội cũ
DELETE FROM reactions;
DELETE FROM files;
DELETE FROM notifications;
DELETE FROM reply_comments;
DELETE FROM comments;
DELETE FROM posts;


-- Xóa người dùng và vai trò (Cha)
DELETE FROM users;
DELETE FROM roles;
DELETE FROM permissions;

-- === 2. TẠO DỮ LIỆU NỀN TẢNG (Users, Roles, Permissions) ===

-- Tạo Roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ADMIN'),(3, 'ROLE_GIAO_VIEN');

-- Tạo Permissions
INSERT INTO permissions (id, name) VALUES
(1, 'post:create'),
(2, 'post:update_all'),
(3, 'post:delete_all'),
(4, 'comment:create'),
(5, 'comment:update_all'),
(6, 'comment:delete_all'),
-- Permissions cho Lịch học
(7, 'schedule:create'),
(8, 'schedule:update'),
(9, 'schedule:delete'),
(10, 'schedule:read_all');

-- Tạo Users
INSERT INTO users (id, username, password, email) VALUES
(1, 'post_owner', '{noop}password', 'owner@example.com'),
(2, 'another_user', '{noop}password', 'another@example.com'),
(3, 'admin_user', '{noop}password', 'admin@example.com'),

-- Giảng viên cần thiết cho Lịch học
(5, 'giaovien1', '{noop}password', 'gv1@example.com'),
(6, 'giaovien2', '{noop}password', 'gv2@example.com'),
(7, 'giaovien3', '{noop}password', 'gv3@example.com');


-- Gán Roles cho Users
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (2, 1), (3, 2),
-- Gán ROLE_USER cho giảng viên (Giả sử họ là user thường)
(5, 1), (6, 1), (7, 1);

-- Gán Permissions cho Roles
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 4), -- ROLE_USER (post:create, comment:create)
(2, 2), (2, 3), (2, 5), (2, 6), -- ADMIN (quản lý Comment/Post)
-- ADMIN có quyền quản lý lịch học
(2, 7), (2, 8), (2, 9), (2, 10);


-- === 3. TẠO DỮ LIỆU NGHIỆP VỤ (Lịch học và Mạng xã hội) ===

-- A. Dữ liệu Chương trình/Module (Cho Lịch học)

-- Tạo Program (ID 100)
INSERT INTO programs (id, name, description) VALUES
(100, 'Khóa học Backend', 'Phát triển ứng dụng với Spring Boot.');

-- Tạo Modules (ID 200, 201)
INSERT INTO modules (id, name, description) VALUES
(200, 'Module Spring JPA', 'Hướng dẫn lập trình cơ sở dữ liệu.'),
(201, 'Module Spring Security', 'Hướng dẫn bảo mật ứng dụng.'),
(202, 'Module Test Mới', 'Dùng cho test case CREATE thành công');

-- Tạo Classes (Lớp học)
-- Cần Class ID 10 và 11 cho Test Case
INSERT INTO classes (id, name, description, program_id) VALUES
(10, 'BE Class K10', 'Lớp học Test A', 100),
(11, 'FE Class K11', 'Lớp học Test B', 100);

-- Gán Module vào Program (Cần thiết cho Logic nghiệp vụ sau này)
INSERT INTO program_modules (program_id, module_id, position)
VALUES (100, 200, 1), (100, 201, 2);


-- B. Dữ liệu LỊCH HỌC (Cho Test Conflict)

-- Schedule ID 1000: Dùng để kiểm tra UPDATE và TRÙNG LỊCH.
-- Lịch 1: Class 10, Module 200, GV 5. Ngày: 2025-11-01 -> 2025-11-15
INSERT INTO class_module_schedules (id, class_id, module_id, start_date, end_date, instructor_id, status)
VALUES
(1000, 10, 200, '2025-11-01', '2025-11-15', 5, 'planned'),
-- Lịch 2: Dùng để tránh trùng lặp Class/Module. Class 11, Module 201, GV 6.
(1001, 11, 201, '2025-12-01', '2025-12-10', 6, 'planned');


-- C. Dữ liệu Mạng xã hội (Cho các test case Comment cũ)

-- Tạo Posts
INSERT INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
(10, 1, 'Bài viết của owner', 'open'),
(11, 1, 'Bài viết private của owner', 'only_me'),
(12, 2, 'Bài viết của user khác', 'friends');

-- Tạo Comments
INSERT INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(20, 10, 2, 'Bình luận gốc của user 2', NOW()),
(21, 12, 1, 'Bình luận của user 1 vào post của user 2', NOW());

-- Tạo Reply Comments
INSERT INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(30, 20, 1, 'Reply của user 1', NOW());

-- Tạo Notifications (Nếu cần)
INSERT INTO notifications (id, receiver_id, sender_id, notification_type, title, content, created_at, is_read, read_at, target_type, target_id) VALUES
(50, 1, 2, 'POST_COMMENT', 'Thông báo mới', 'User 2 đã bình luận bài viết của bạn', NOW(), false, null, 'POST', 10),
(51, 1, 3, 'SYSTEM_ANNOUNCEMENT', 'Thông báo hệ thống', 'Chào mừng bạn đến với hệ thống', DATEADD('DAY', -1, NOW()), true, DATEADD('HOUR', -12, NOW()), null, null);