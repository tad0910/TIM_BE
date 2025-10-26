-- Xóa dữ liệu cũ
DELETE FROM user_roles;
DELETE FROM roles;
DELETE FROM posts;
DELETE FROM users;

-- Tạo Roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ADMIN');

-- Tạo Users
INSERT INTO users (id, username, password, email) VALUES
(1, 'post_owner', '{noop}password', 'owner@example.com'),
(2, 'another_user', '{noop}password', 'another@example.com'),
(3, 'admin_user', '{noop}password', 'admin@example.com');

-- Gán Roles cho Users
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- post_owner có ROLE_USER
(2, 1), -- another_user có ROLE_USER
(3, 2); -- admin_user có ROLE_ADMIN

-- Tạo Posts
INSERT INTO posts (id, nguoi_dung_id, content, quyen_rieng_tu) VALUES
(10, 1, 'Bài viết của owner', 'open'),
(11, 2, 'Bài viết của user khác', 'friends');