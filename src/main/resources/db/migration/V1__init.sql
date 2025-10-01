-- USERS table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    branch VARCHAR(100) NOT NULL,
    roll_number VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL
);

-- EVENTS table
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255),
    organiser_id BIGINT NOT NULL,
    date_time TIMESTAMP NOT NULL,
    image_url VARCHAR(500),
    likes_count INT DEFAULT 0,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    visibility_type VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    visibility_value VARCHAR(128),
    CONSTRAINT fk_organiser FOREIGN KEY (organiser_id) REFERENCES users(id) ON DELETE CASCADE
);

-- LIKES table
CREATE TABLE likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_event (user_id, event_id)
);

-- EVENT_INVITEES table (for private events)
CREATE TABLE event_invitees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
