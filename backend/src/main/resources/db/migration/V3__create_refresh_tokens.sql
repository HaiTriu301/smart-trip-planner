-- V3: refresh_tokens (design.md 5.2). Mirrored 1:1 by model/RefreshToken.java (ddl-auto=validate).
-- token_hash is the SHA-256 hex of the token the client holds; the raw token is never stored.

CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NULL,
    user_agent  VARCHAR(255) NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_id (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
