-- Migration: drop 'code' column and enforce UNIQUE on 'name' for gamification_behaviors
-- Please backup your database before running.

-- Drop index on code if exists (MySQL 5.7+ supports IF EXISTS)
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'unique_code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX unique_code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop code column if exists
SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND column_name = 'code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP COLUMN code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure unique constraint on name
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'unique_name');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX unique_name', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE gamification_behaviors ADD UNIQUE KEY unique_name (name);


