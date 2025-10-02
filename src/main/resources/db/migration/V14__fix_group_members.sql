-- V14__fix_group_members.sql

-- 1. Drop the incorrect group_members table (with FK → groups)
DROP TABLE IF EXISTS group_members;

-- 2. Create the correct group_members table (FK → user_groups)
CREATE TABLE group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'MEMBER',
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES user_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_user UNIQUE (group_id, user_id)
);
