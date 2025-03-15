-- Add notify_on_completion column to dependency_analysis table
ALTER TABLE dependency_analysis
    ADD COLUMN notify_on_completion BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing records to have notify_on_completion set to FALSE
UPDATE dependency_analysis
SET notify_on_completion = FALSE
WHERE notify_on_completion IS NULL;