-- Add show_notifications column to app_settings table
ALTER TABLE app_settings
ADD COLUMN show_notifications BOOLEAN NOT NULL DEFAULT TRUE;

-- Update existing records to have show_notifications set to TRUE
UPDATE app_settings
SET show_notifications = TRUE
WHERE show_notifications IS NULL; 