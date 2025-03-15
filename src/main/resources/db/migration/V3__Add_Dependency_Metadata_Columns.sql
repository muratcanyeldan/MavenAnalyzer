-- Add scope, license, and status columns to the dependencies table
ALTER TABLE dependencies ADD COLUMN scope VARCHAR(50);
ALTER TABLE dependencies ADD COLUMN license VARCHAR(100);
ALTER TABLE dependencies ADD COLUMN status VARCHAR(50);

-- Set default values for existing dependencies
UPDATE dependencies SET scope = 'compile' WHERE scope IS NULL;
UPDATE dependencies SET license = 'unknown' WHERE license IS NULL;
UPDATE dependencies SET status = 
    CASE 
        WHEN is_vulnerable = true THEN 'Vulnerable'
        WHEN is_outdated = true THEN 'Outdated'
        WHEN latest_version IS NOT NULL THEN 'Up-to-date'
        ELSE 'Unknown'
    END
WHERE status IS NULL; 