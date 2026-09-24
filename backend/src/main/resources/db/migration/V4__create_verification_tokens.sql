-- V4: verification_tokens (design.md 5.2). Mirrored 1:1 by model/VerificationToken.java (ddl-auto=validate).
-- One-time tokens for email verification (24h) and password reset (1h); only the SHA-256 hex is stored.

CREATE TABLE verification_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    type        ENUM('EMAIL_VERIFY', 'PASSWORD_RESET') NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_verification_tokens_token_hash (token_hash),
    KEY idx_verification_tokens_user_type (user_id, type),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
