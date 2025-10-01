-- Add type column to groups table
ALTER TABLE `groups`
ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'USER_CREATED';
