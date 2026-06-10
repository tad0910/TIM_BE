CREATE TABLE gamification_point_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,         
    description TEXT,                   
    max_points INT DEFAULT 0,         
    image_url VARCHAR(255),           
    is_active BOOLEAN DEFAULT TRUE,     
    show_on_dashboard BOOLEAN DEFAULT TRUE, 
    created_by INT,                    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE gamification_behavior_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,        
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gamification_behaviors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,             
    name VARCHAR(255) UNIQUE NOT NULL,        

    frequency_type ENUM('UNLIMITED', 'DAILY', 'WEEKLY', 'MONTHLY', 'ONCE') DEFAULT 'UNLIMITED',
    max_times_per_frequency INT DEFAULT 1, 
    
    -- CÁC TRƯỜNG CŨ (DEPRECATED - Giữ lại để backward compatibility)
    -- Khuyến nghị: Sử dụng bảng behavior_point_types thay vì các trường này
    -- Các trường này vẫn hoạt động nhưng sẽ bị thay thế bởi behavior_point_types
    point_diligence INT DEFAULT 0,      -- DEPRECATED: Dùng behavior_point_types thay thế
    point_competence INT DEFAULT 0,     -- DEPRECATED: Dùng behavior_point_types thay thế
    point_experience INT DEFAULT 0,      -- DEPRECATED: Dùng behavior_point_types thay thế
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES gamification_behavior_groups(id)
);

CREATE TABLE behavior_point_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    behavior_id INT NOT NULL,
    point_type_id INT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    notification_template_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (behavior_id) REFERENCES gamification_behaviors(id) ON DELETE CASCADE,
    FOREIGN KEY (point_type_id) REFERENCES gamification_point_types(id),
    FOREIGN KEY (notification_template_id) REFERENCES notification_templates(id),
    UNIQUE KEY unique_behavior_point_type (behavior_id, point_type_id)
);

CREATE TABLE gamification_achievements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,        
    image_url VARCHAR(255),             
    created_by INT,                     
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gamification_achievement_levels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    achievement_id INT NOT NULL,        
    level_name VARCHAR(100) NOT NULL,   

    required_point_type_id INT,         
                                       
    min_points_required INT DEFAULT 0,  
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES gamification_achievements(id)

);

CREATE TABLE user_point_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,              
    behavior_id INT,                  

    points_diligence_earned INT DEFAULT 0,
    points_competence_earned INT DEFAULT 0,
    points_experience_earned INT DEFAULT 0,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (behavior_id) REFERENCES gamification_behaviors(id)
);

CREATE TABLE user_gamification_stats (
    user_id INT PRIMARY KEY,
    total_diligence INT DEFAULT 0,      
    total_competence INT DEFAULT 0,     
    total_experience INT DEFAULT 0,     
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    achievement_level_id INT NOT NULL, 
    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_displayed BOOLEAN DEFAULT FALSE, 
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (achievement_level_id) REFERENCES gamification_achievement_levels(id)
);

ALTER TABLE `notifications` 
MODIFY COLUMN `notification_type` ENUM(
    -- Các giá trị cũ (GIỮ NGUYÊN)
    'POST_REACTION','POST_COMMENT','COMMENT_REACTION','COMMENT_REPLY',
    'REPLY_REACTION','USER_FOLLOW','POST_MENTION','COMMENT_MENTION',
    'SYSTEM_ANNOUNCEMENT','LATE_ATTENDANCE_OPENED','ATTENDANCE_REMINDER_LATE',
    'ATTENDANCE_REMINDER_ENDING','GRADE_NEW','GRADE_UPDATED','BLOG_NEW',
    'TUITION_OVERDUE','TUITION_REMINDER',
    
    -- CÁC GIÁ TRỊ MỚI CHO GAMIFICATION (BỔ SUNG)
    'GAMIFICATION_POINT_EARNED',      -- Khi nhận điểm (Bất kể loại điểm nào)
    'GAMIFICATION_ACHIEVEMENT_UNLOCKED', -- Khi đạt cấp bậc thành tích mới
    'GAMIFICATION_LEVEL_UP',          -- (Tùy chọn) Nếu sau này có Level tổng của User
    'GAMIFICATION_RANKING_CHANGE'     -- (Tùy chọn) Khi lọt Top BXH
) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;