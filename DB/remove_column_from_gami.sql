ALTER TABLE gamification_behaviors DROP INDEX code;
ALTER TABLE gamification_behaviors DROP COLUMN code;
ALTER TABLE gamification_behaviors ADD UNIQUE KEY unique_name (name);