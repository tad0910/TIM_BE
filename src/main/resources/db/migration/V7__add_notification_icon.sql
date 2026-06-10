-- Add icon_url to notifications to store icon from notification templates
ALTER TABLE notifications
ADD COLUMN icon_url VARCHAR(500) NULL;

