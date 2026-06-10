ALTER TABLE users
    ADD COLUMN job_interest_enabled TINYINT(1) NOT NULL DEFAULT 0;

UPDATE users
SET job_interest_enabled = 0
WHERE job_interest_enabled IS NULL;
