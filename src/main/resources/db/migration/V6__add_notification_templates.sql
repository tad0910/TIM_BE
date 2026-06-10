-- Migration: Add notification template columns to gamification_behaviors and gamification_achievement_levels
-- Date: 2025-01-10

-- Add notification template columns to gamification_behaviors table
ALTER TABLE gamification_behaviors
ADD COLUMN notification_template_diligence_id BIGINT NULL,
ADD COLUMN notification_template_competence_id BIGINT NULL,
ADD COLUMN notification_template_experience_id BIGINT NULL;

-- Add foreign key constraints for notification templates in behaviors
ALTER TABLE gamification_behaviors
ADD CONSTRAINT fk_behavior_notification_template_diligence
    FOREIGN KEY (notification_template_diligence_id) 
    REFERENCES notification_templates(id) 
    ON DELETE SET NULL,
ADD CONSTRAINT fk_behavior_notification_template_competence
    FOREIGN KEY (notification_template_competence_id) 
    REFERENCES notification_templates(id) 
    ON DELETE SET NULL,
ADD CONSTRAINT fk_behavior_notification_template_experience
    FOREIGN KEY (notification_template_experience_id) 
    REFERENCES notification_templates(id) 
    ON DELETE SET NULL;

-- Add notification template column to gamification_achievement_levels table
ALTER TABLE gamification_achievement_levels
ADD COLUMN notification_template_id BIGINT NULL;

-- Add foreign key constraint for notification template in achievement levels
ALTER TABLE gamification_achievement_levels
ADD CONSTRAINT fk_achievement_level_notification_template
    FOREIGN KEY (notification_template_id) 
    REFERENCES notification_templates(id) 
    ON DELETE SET NULL;

