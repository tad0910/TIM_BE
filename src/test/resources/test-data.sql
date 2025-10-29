-- Xóa dữ liệu cũ (ĐÚNG THỨ TỰ)
DELETE FROM role_permissions; -- Xóa bảng trung gian trước
DELETE FROM user_roles;     -- Xóa bảng trung gian trước

DELETE FROM reactions;      -- **1. Xóa reactions trước**
DELETE FROM files;          -- **2. Xóa files trước** (vì nó tham chiếu đến post, comment, reply)
DELETE FROM notifications;  -- **3. Xóa notifications trước**

DELETE FROM reply_comments; -- **4. Giờ mới xóa replies** (sau khi reactions đã xóa)
DELETE FROM comments;      -- (Nếu có khóa ngoại, xóa trước)
-- Xóa các bảng con khác có khóa ngoại...

DELETE FROM posts;          -- Giờ mới xóa post
DELETE FROM users;          -- Giờ mới xóa user
DELETE FROM roles;          -- Xóa roles và permissions sau cùng
DELETE FROM permissions;

-- (Phần INSERT giữ nguyên)
-- Tạo Roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ADMIN');

-- Tạo Permissions
INSERT INTO permissions (id, name) VALUES
(1, 'post:create'),
(2, 'post:update_all'),
(3, 'post:delete_all'),
(4, 'comment:create'),
(5, 'comment:update_all'),
(6, 'comment:delete_all');

-- Tạo Users
INSERT INTO users (id, username, password, email) VALUES
(1, 'post_owner', '{noop}password', 'owner@example.com'),
(2, 'another_user', '{noop}password', 'another@example.com'),
(3, 'admin_user', '{noop}password', 'admin@example.com');

-- Gán Roles cho Users
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (2, 1), (3, 2);

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

-- Gán Permissions cho Roles
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 4), (2, 2), (2, 3), (2, 5), (2, 6);

INSERT INTO notifications (id, receiver_id, sender_id, notification_type, title, content, created_at, is_read, read_at, target_type, target_id) VALUES
(50, 1, 2, 'POST_COMMENT', 'Thông báo mới', 'User 2 đã bình luận bài viết của bạn', NOW(), false, null, 'POST', 10),
(51, 1, 3, 'SYSTEM_ANNOUNCEMENT', 'Thông báo hệ thống', 'Chào mừng bạn đến với hệ thống', DATEADD('DAY', -1, NOW()), true, DATEADD('HOUR', -12, NOW()), null, null);