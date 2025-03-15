-- Add notify_on_completion column to dependency_analyses table
ALTER TABLE dependency_analyses
    ADD COLUMN notify_on_completion BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing records to have notify_on_completion set to FALSE
UPDATE dependency_analyses
SET notify_on_completion = FALSE
WHERE notify_on_completion IS NULL;