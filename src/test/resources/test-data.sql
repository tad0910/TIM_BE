
DELETE FROM role_permissions;
DELETE FROM user_roles;

DELETE FROM class_module_schedules;
DELETE FROM module_sessions;
DELETE FROM program_modules;
DELETE FROM class_members;
DELETE FROM classes;
DELETE FROM modules;
DELETE FROM programs;


DELETE FROM reactions;
DELETE FROM files;
DELETE FROM notifications;
DELETE FROM reply_comments;
DELETE FROM comments;
DELETE FROM posts;


DELETE FROM users;
DELETE FROM roles;
DELETE FROM permissions;


INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ADMIN'),(3, 'ROLE_GIAO_VIEN');

INSERT INTO permissions (id, name) VALUES
(1, 'post:create'),
(2, 'post:update_all'),
(3, 'post:delete_all'),
(4, 'comment:create'),
(5, 'comment:update_all'),
(6, 'comment:delete_all'),
(7, 'schedule:create'),
(8, 'schedule:update'),
(9, 'schedule:delete'),
(10, 'schedule:read_all'),
(11, 'program:create'),
(12, 'class:create'),
(13, 'class:read_all'),
(14, 'class:delete_all'),
(15, 'user:update_all'),
(16, 'module:update'),
(17, 'module:create'),
(18, 'user:read_all'),
(19, 'user:create'),
(20, 'user:delete'),
(21, 'reaction:delete'),
(22, 'program:update'),
(23, 'program:delete'),
(24, 'notification:cleanup'),
(25, 'notification:create_manual'),
(26, 'module:delete'),
(27, 'class:update_all');


INSERT INTO users (id, username, password, email, deleted) VALUES
(1, 'post_owner', '{noop}password', 'owner@example.com', false),
(2, 'another_user', '{noop}password', 'another@example.com', false),
(3, 'admin_user', '{noop}password', 'admin@example.com', false),

(5, 'giaovien1', '{noop}password', 'gv1@example.com', false),
(6, 'giaovien2', '{noop}password', 'gv2@example.com', false),
(7, 'giaovien3', '{noop}password', 'gv3@example.com', false);


INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (2, 1), (3, 2),
(5, 1), (6, 1), (7, 1);

INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 11),
(2, 12),
(1, 1), (1, 4),
(2, 2), (2, 3), (2, 5), (2, 6),
(2, 7), (2, 8), (2, 9), (2, 10), (2, 13), (2, 14), (2, 15), (2, 16), (2, 17), (2, 18), (2, 19), (2, 20), (2, 21), (2, 22), (2, 23), (2, 24), (2, 25), (2, 26), (2, 27);


INSERT INTO programs (id, name, description) VALUES
(100, 'Khóa học Backend', 'Phát triển ứng dụng với Spring Boot.');

INSERT INTO modules (id, name, description) VALUES
(200, 'Module Spring JPA', 'Hướng dẫn lập trình cơ sở dữ liệu.'),
(201, 'Module Spring Security', 'Hướng dẫn bảo mật ứng dụng.'),
(202, 'Module Test Mới', 'Dùng cho test case CREATE thành công');

INSERT INTO module_sessions (id, module_id, session_number, title, content)
VALUES
(300, 200, 1, 'Buổi 1: Giới thiệu JPA', 'Nội dung buổi 1'),
(301, 200, 2, 'Buổi 2: Quan hệ Entity', 'Nội dung buổi 2'),
(302, 201, 1, 'Buổi 1: Giới thiệu Security', 'Nội dung buổi 1 Security'),
(303, 202, 1, 'Buổi 1: Module Test Mới', 'Nội dung buổi 1 Module Test Mới');

INSERT INTO classes (id, name, description, program_id) VALUES
(10, 'BE Class K10', 'Lớp học Test A', 100),
(11, 'FE Class K11', 'Lớp học Test B', 100);

INSERT INTO class_members (lop_id, nguoi_dung_id, vai_tro, ngay_tham_gia) VALUES
(10, 1, 'sinh_vien', NOW()),
(10, 5, 'giao_vien', NOW()),
(10, 2, 'sinh_vien', NOW());

INSERT INTO program_modules (program_id, module_id, position)
VALUES (100, 200, 1), (100, 201, 2);


INSERT INTO class_module_schedules (id, class_id, module_id, start_date, end_date, instructor_id, status)
VALUES
(1000, 10, 200, '2025-11-01', '2025-11-15', 5, 'planned'),
(1001, 11, 201, '2025-12-01', '2025-12-10', 6, 'planned');


INSERT INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
(10, 1, 'Bài viết của owner', 'open'),
(11, 1, 'Bài viết private của owner', 'only_me'),
(12, 2, 'Bài viết của user khác', 'friends');

INSERT INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(20, 10, 2, 'Bình luận gốc của user 2', NOW()),
(21, 12, 1, 'Bình luận của user 1 vào post của user 2', NOW());

INSERT INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(30, 20, 1, 'Reply của user 1', NOW());

INSERT INTO reactions (id, nguoi_dung_id, bai_viet_id, comment_id, reply_comment_id, loai_cam_xuc)
VALUES
(100, 2, 10, null, null, 'like'),
(101, 1, null, 20, null, 'love'),
(102, 1, null, null, 30, 'haha');

INSERT INTO notifications (id, receiver_id, sender_id, notification_type, title, content, created_at, is_read, read_at, target_type, target_id) VALUES
(50, 1, 2, 'POST_COMMENT', 'Thông báo mới', 'User 2 đã bình luận bài viết của bạn', NOW(), false, null, 'POST', 10),
(51, 1, 3, 'SYSTEM_ANNOUNCEMENT', 'Thông báo hệ thống', 'Chào mừng bạn đến với hệ thống', DATEADD('DAY', -1, NOW()), true, DATEADD('HOUR', -12, NOW()), null, null);