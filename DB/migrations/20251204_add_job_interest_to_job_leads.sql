ALTER TABLE job_leads
    ADD COLUMN job_interest TINYINT(1) NOT NULL DEFAULT 1;

UPDATE job_leads
SET job_interest = 1
WHERE job_interest IS NULL;
