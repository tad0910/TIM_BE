-- Migration: drop 'code' column and enforce UNIQUE on 'name' for gamification_behaviors
-- Please backup your database before running.

-- Drop index on code if exists
ALTER TABLE gamification_behaviors DROP INDEX IF EXISTS code;
ALTER TABLE gamification_behaviors DROP INDEX IF EXISTS unique_code;

-- Drop code column if exists
ALTER TABLE gamification_behaviors DROP COLUMN IF EXISTS code;

-- Ensure unique constraint on name
ALTER TABLE gamification_behaviors DROP INDEX IF EXISTS unique_name;
ALTER TABLE gamification_behaviors ADD UNIQUE KEY unique_name (name);


