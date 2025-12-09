-- ============================================
-- Migration Script: Drop 'code' column from gamification_behaviors
-- Database: MariaDB/MySQL
-- Safe to run multiple times (checks before dropping)
-- ============================================

-- Step 1: Drop index on 'code' if exists
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Drop index 'unique_code' if exists
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'unique_code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX unique_code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Drop 'code' column if exists
SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND column_name = 'code');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP COLUMN code', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 4: Drop existing 'unique_name' index if exists (to avoid error if re-running)
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
               AND table_name = 'gamification_behaviors' 
               AND index_name = 'unique_name');
SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE gamification_behaviors DROP INDEX unique_name', 'SELECT 1');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 5: Add unique constraint on 'name' column
ALTER TABLE gamification_behaviors ADD UNIQUE KEY unique_name (name);

-- Verification: Check if migration was successful
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN 'SUCCESS: Column "code" has been removed'
        ELSE 'WARNING: Column "code" still exists'
    END AS migration_status
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
  AND table_name = 'gamification_behaviors' 
  AND column_name = 'code';

SELECT 'Migration completed!' AS result;

