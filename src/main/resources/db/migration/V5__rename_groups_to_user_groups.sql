-- -- Rename table groups -> user_groups (only if groups exists)
-- RENAME TABLE `groups` TO `user_groups`;

-- -- Drop old foreign key constraint from group_members
-- ALTER TABLE group_members DROP FOREIGN KEY fk_group;

-- -- Add new foreign key pointing to user_groups
-- ALTER TABLE group_members
-- ADD CONSTRAINT fk_group FOREIGN KEY (group_id) REFERENCES user_groups(id) ON DELETE CASCADE;

select 1;