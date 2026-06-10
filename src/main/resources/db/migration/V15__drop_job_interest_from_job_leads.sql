-- Remove legacy job_leads.job_interest column if it exists
SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'job_leads'
              AND COLUMN_NAME = 'job_interest'
        ),
        'ALTER TABLE job_leads DROP COLUMN job_interest',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
