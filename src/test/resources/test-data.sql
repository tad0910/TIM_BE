DELETE FROM role_permissions;
DELETE FROM user_roles;

DELETE FROM grade_history;
DELETE FROM grades;
DELETE FROM class_module_teacher;
DELETE FROM class_module_schedules;
DELETE FROM module_sessions;
DELETE FROM program_modules;
DELETE FROM class_members;
DELETE FROM student_forms;

DELETE FROM ranking_monthly;
DELETE FROM ranking;
DELETE FROM class_module;
DELETE FROM classes;
DELETE FROM modules;
DELETE FROM programs;

DELETE FROM reactions;
DELETE FROM files;
DELETE FROM notifications;
DELETE FROM reply_comments;
DELETE FROM comments;

DELETE FROM form_templates;
DELETE FROM posts;
DELETE FROM users;
DELETE FROM roles;
DELETE FROM permissions;

INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_USER'), 
(2, 'ROLE_ADMIN'),
(3, 'ROLE_GIAO_VIEN'),
(4, 'ROLE_GIAO_VU'),
(5, 'ROLE_KE_TOAN');

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
(27, 'class:update_all'),
(28, 'grade:read_all'),
(29, 'grade:update'),
(30, 'grade:create'),
(31, 'grade:read_detail'),
(32, 'attendance:open'),
(33, 'attendance:mark'),
(34, 'attendance:read_all'),
(35, 'grade:delete'),
(36, 'form:create'),
(37, 'form:read_all'),
(38, 'form:approve'),
(39, 'form:delete');

INSERT INTO users (id, username, password, email, deleted, job_interest_enabled) VALUES
(1, 'post_owner', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'owner@example.com', false, false),
(2, 'another_user', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'another@example.com', false, false),
(3, 'admin_user', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'admin@example.com', false, false),
(4, 'stranger_user', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'stranger@example.com', false, false),
(5, 'giaovien1', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'gv1@example.com', false, false),
(6, 'giaovien2', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'gv2@example.com', false, false),
(7, 'giaovien3', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'gv3@example.com', false, false),
(8, 'form_student', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'form.student@example.com', false, false),
(9, 'form_admin', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'form.admin@example.com', false, false),
(10, 'giaovu_user', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'giaovu@example.com', false, false),
(11, 'ketoan_user', '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica', 'ketoan@example.com', false, false);

UPDATE users SET firstname = 'Owner', lastname = 'Post', so_dien_thoai = '0911000001' WHERE id = 1;
UPDATE users SET firstname = 'Student', lastname = 'Form', so_dien_thoai = '0911000008' WHERE id = 8;
UPDATE users SET firstname = 'Admin', lastname = 'Form', so_dien_thoai = '0911000009' WHERE id = 9;
UPDATE users SET firstname = 'Coach', lastname = 'Teacher', so_dien_thoai = '0911000005' WHERE id = 5;
UPDATE users SET firstname = 'Academic', lastname = 'Officer', so_dien_thoai = '0911000010' WHERE id = 10;
UPDATE users SET firstname = 'Account', lastname = 'Ant', so_dien_thoai = '0911000011' WHERE id = 11;

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 1),
(3, 2),
(4, 1),
(5, 3),
(6, 1),
(7, 1),
(8, 1),
(9, 2),
(10, 4),
(11, 5);

INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 11),
(2, 12),
(1, 1),
(1, 4),
(2, 2),
(2, 3),
(2, 5),
(2, 6),
(2, 7),
(2, 8),
(2, 9),
(2, 10),
(2, 13),
(2, 14),
(2, 15),
(2, 16),
(2, 17),
(2, 18),
(2, 19),
(2, 20),
(2, 21),
(2, 22),
(2, 23),
(2, 24),
(2, 25),
(2, 26),
(2, 27),
(2, 28),
(2, 29),
(3, 29),
(3, 30),
(3, 31),
(3, 35),
(1, 36),
(2, 36),
(2, 37),
(2, 38),
(2, 39),
(3, 37),
(3, 38),
(4, 37),
(4, 38),
(4, 39),
(5, 37),
(5, 38);

INSERT INTO programs (id, name, description) VALUES
(100, 'Khóa học Backend', 'Phát triển ứng dụng với Spring Boot.');

INSERT INTO modules (id, name, description) VALUES
(200, 'Module Spring JPA', 'Hướng dẫn lập trình cơ sở dữ liệu.'),
(201, 'Module Spring Security', 'Hướng dẫn bảo mật ứng dụng.'),
(202, 'Module Test Mới', 'Dùng cho test case CREATE thành công');

INSERT INTO module_sessions (id, module_id, session_number, title, content) VALUES
(300, 200, 1, 'Buổi 1: Giới thiệu JPA', 'Nội dung buổi 1'),
(301, 200, 2, 'Buổi 2: Quan hệ Entity', 'Nội dung buổi 2'),
(302, 201, 1, 'Buổi 1: Giới thiệu Security', 'Nội dung buổi 1 Security'),
(303, 202, 1, 'Buổi 1: Module Test Mới', 'Nội dung buổi 1 Module Test Mới');

INSERT INTO classes (id, name, description, program_id, jobs_enabled) VALUES
(10, 'BE Class K10', 'Lớp học Test A', 100, false),
(11, 'FE Class K11', 'Lớp học Test B', 100, false),
(12, 'Form Class K12', 'Lớp học Test C', 100, false),
(13, 'Form Class K13', 'Lớp học Test D', 100, false);

INSERT INTO class_module (id, class_id, module_id, schedule_type) VALUES
(500, 10, 200, 'fixed');

INSERT INTO class_members (lop_id, nguoi_dung_id, vai_tro, ngay_tham_gia) VALUES
(10, 1, 'sinh_vien', NOW()),
(10, 5, 'giao_vien', NOW()),
(10, 2, 'sinh_vien', NOW()),
(10, 8, 'sinh_vien', NOW());

INSERT INTO program_modules (program_id, module_id, position) VALUES
(100, 200, 1),
(100, 201, 2);

INSERT INTO class_module_schedules (id, class_id, module_id, class_module_id, start_date, end_date, instructor_id, status) VALUES
(1000, 10, 200, 500, '2025-11-01 00:00:00', '2025-11-08 00:00:00', 5, 'planned');


INSERT INTO class_module_teacher (class_module_id, user_id) VALUES
(500, 5);

INSERT INTO posts (id, nguoi_dung_id, noi_dung, quyen_rieng_tu) VALUES
(10, 1, 'Bài viết của owner', 'open'),
(11, 1, 'Bài viết private của owner', 'only_me'),
(12, 2, 'Bài viết của user khác', 'friends');

INSERT INTO form_templates (id, code, name, description, is_active) VALUES
(1000, 'RESERVATION', 'Đơn Bảo lưu', 'Dùng cho học viên xin bảo lưu', true),
(1001, 'TRANSFER', 'Đơn Chuyển lớp', 'Dùng cho học viên muốn chuyển lớp', true),
(1002, 'DROPOUT', 'Đơn Thôi học', 'Dùng cho học viên thôi học', false);

INSERT INTO student_forms (
    id, template_id, student_id, class_id, created_by_user_id,
    reason, start_date, end_date, fee_amount,
    coach_approval, academic_approval, accountant_approval, admin_approval,
    status
) VALUES
(2000, 1000, 8, 10, 9,
 'Xin bảo lưu 1 tháng', '2025-01-01', '2025-02-01', 0,
 'PENDING', 'PENDING', 'PENDING', 'PENDING',
 'PENDING'),
(2001, 1001, 8, 12, 9,
 'Xin chuyển lớp khác', '2025-03-01', '2025-03-15', 500000,
 'APPROVED', 'PENDING', 'PENDING', 'PENDING',
 'PROCESSING');

INSERT INTO comments (id, bai_viet_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(20, 10, 2, 'Bình luận gốc của user 2', NOW()),
(21, 12, 1, 'Bình luận của user 1 vào post của user 2', NOW());

INSERT INTO reply_comments (id, comments_id, nguoi_dung_id, noi_dung, thoi_gian_tao) VALUES
(30, 20, 1, 'Reply của user 1', NOW());

INSERT INTO reactions (id, nguoi_dung_id, bai_viet_id, comment_id, reply_comment_id, loai_cam_xuc) VALUES
(100, 2, 10, null, null, 'like'),
(101, 1, null, 20, null, 'love'),
(102, 1, null, null, 30, 'haha');

INSERT INTO notifications (id, receiver_id, sender_id, notification_type, title, content, created_at, is_read, read_at, target_type, target_id) VALUES
(50, 1, 2, 'POST_COMMENT', 'Thông báo mới', 'User 2 đã bình luận bài viết của bạn', NOW(), false, null, 'POST', 10),
(51, 1, 3, 'SYSTEM_ANNOUNCEMENT', 'Thông báo hệ thống', 'Chào mừng bạn đến với hệ thống', DATEADD('DAY', -1, NOW()), true, DATEADD('HOUR', -12, NOW()), null, null);

INSERT INTO grades (id, class_module_id, student_id, theory_score, practice_score, entry_date, entered_by_user_id, status, created_at, updated_at) VALUES
(1, 500, 1, 8.0, 7.5, CURRENT_DATE, 5, 'ACTIVE', NOW(), NOW()),
(2, 500, 2, 7.0, 8.0, CURRENT_DATE, 5, 'ACTIVE', NOW(), NOW());



DELETE FROM user_point_logs;
DELETE FROM behavior_point_types;
DELETE FROM gamification_behaviors;
DELETE FROM gamification_behavior_groups;
DELETE FROM attendance_records;
DELETE FROM attendance_sessions;

INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 32),
(2, 33),
(2, 34);


INSERT INTO class_module_schedules (id, class_id, module_id, class_module_id, start_date, end_date, instructor_id, status) VALUES
(1001, 10, 200, 500, '2025-11-02 00:00:00', '2025-11-09 00:00:00', 5, 'planned');

INSERT INTO attendance_sessions (id, schedule_id, opened_by, is_late, opened_at) VALUES
(1, 1001, 5, false, '2025-11-02 00:05:00');

INSERT INTO attendance_records (marked_by, student_id, schedule_id, status, marked_at, notes) VALUES
(5, 1, 1001, 'present', NOW(), 'Bản ghi điểm danh có sẵn dùng cho test mở lại phiên');

-- Gamification test data
INSERT INTO gamification_behavior_groups (id, name, created_at) VALUES
(1, 'Học tập', NOW());

INSERT INTO gamification_behaviors (id, group_id, name, frequency_type, max_times_per_frequency, point_diligence, point_competence, point_experience, created_at) VALUES
(1, 1, 'ATTEND_ON_TIME', 'DAILY', 1, 10, 0, 5, NOW()),
(2, 1, 'GIVING_SCORES', 'UNLIMITED', 1, 0, 5, 0, NOW()),
(3, 1, 'HIGH_POINT_1', 'ONCE', 1, 0, 0, 20, NOW()),
(4, 1, 'HIGH_POINT_2', 'ONCE', 1, 0, 0, 50, NOW()),
(5, 1, 'READ_BLOG', 'UNLIMITED', 1, 0, 0, 5, NOW()),
(6, 1, 'FIRST_POST', 'ONCE', 1, 0, 0, 10, NOW()),
(7, 1, 'POST_SHARE', 'UNLIMITED', 1, 0, 0, 5, NOW()),
(8, 1, 'POST''S_LIKE', 'UNLIMITED', 1, 0, 0, 5, NOW());