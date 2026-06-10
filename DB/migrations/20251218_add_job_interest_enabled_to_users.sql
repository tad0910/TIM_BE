    -- Ensure users.job_interest_enabled exists (default OFF)
    SET @stmt := (
        SELECT IF(
            EXISTS(
                SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'users'
                AND COLUMN_NAME = 'job_interest_enabled'
            ),
            'SELECT 1',
            'ALTER TABLE users ADD COLUMN job_interest_enabled TINYINT(1) NOT NULL DEFAULT 0'
        )
    );
    PREPARE stmt FROM @stmt;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    UPDATE users
    SET job_interest_enabled = 0
    WHERE job_interest_enabled IS NULL;
