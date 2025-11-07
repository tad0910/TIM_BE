-- Migration script to remove template fields from modules and module_sessions tables
-- This makes Module and ModuleSession act as templates only
-- ClassModuleSchedule will inherit these fields instead

USE dbtest;

-- MODULES
ALTER TABLE modules DROP FOREIGN KEY fk_modules_instructor;
ALTER TABLE modules DROP INDEX fk_modules_instructor;
ALTER TABLE modules DROP COLUMN instructor_id;

-- MODULE_SESSIONS
ALTER TABLE module_sessions DROP FOREIGN KEY fk_ms_instructor;
ALTER TABLE module_sessions DROP INDEX fk_ms_instructor;
ALTER TABLE module_sessions DROP COLUMN scheduled_at;
ALTER TABLE module_sessions DROP COLUMN end_date;
ALTER TABLE module_sessions DROP COLUMN status;
ALTER TABLE module_sessions DROP COLUMN instructor_id;


