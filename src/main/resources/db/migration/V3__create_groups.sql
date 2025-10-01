-- Create groups table
CREATE TABLE `groups` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create group_members table
CREATE TABLE group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'MEMBER',
    CONSTRAINT fk_group FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    CONSTRAINT fk_member FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Optional: enforce unique membership (no duplicate user in same group)
ALTER TABLE group_members
ADD CONSTRAINT uq_group_user UNIQUE (group_id, user_id);
