-- Add classes.jobs_enabled (default OFF)
SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'classes'
              AND COLUMN_NAME = 'jobs_enabled'
        ),
        'SELECT 1',
        'ALTER TABLE classes ADD COLUMN jobs_enabled TINYINT(1) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE classes
SET jobs_enabled = 0
WHERE jobs_enabled IS NULL;
