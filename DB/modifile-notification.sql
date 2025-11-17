ALTER TABLE notifications 
MODIFY COLUMN notification_type ENUM(
    'POST_REACTION','POST_COMMENT','COMMENT_REACTION','COMMENT_REPLY',
    'REPLY_REACTION','USER_FOLLOW','POST_MENTION','COMMENT_MENTION',
    'SYSTEM_ANNOUNCEMENT','LATE_ATTENDANCE_OPENED',
    'ATTENDANCE_REMINDER_LATE','ATTENDANCE_REMINDER_ENDING'
) NOT NULL;

INSERT INTO permissions (id, name) VALUES
    (36, 'attendance:open'),
    (37, 'attendance:read_all')

INSERT INTO role_permissions (role_id, permission_id) VALUES
     (1, 36),
     (1, 37),
     (2, 37)
