-- V13__recreate_user_groups.sql

-- 1. Drop the incorrect user_groups table if it exists
DROP TABLE IF EXISTS user_groups;

-- 2. Create the correct user_groups table
CREATE TABLE user_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50) NOT NULL DEFAULT 'USER_CREATED',

    CONSTRAINT fk_user_groups_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);
