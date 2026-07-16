CREATE TABLE refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    session_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    role_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    current_token_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    updated_by VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_sessions_session_id UNIQUE (session_id),
    CONSTRAINT uk_refresh_sessions_current_token_id UNIQUE (current_token_id),
    INDEX idx_refresh_sessions_subject_revoked_expires (subject_id, revoked_at, expires_at),
    INDEX idx_refresh_sessions_expires_id (expires_at, id),
    INDEX idx_refresh_sessions_revoked_id (revoked_at, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_bin;
