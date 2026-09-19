-- V2: users table (design.md 5.2).
-- Column types are mirrored 1:1 by model/User.java; Hibernate runs with ddl-auto=validate against this DDL.
-- DATETIME(6) keeps microseconds, the precision Hibernate uses for java.time.Instant.

CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(120)    NOT NULL,
    avatar_url      VARCHAR(512)    NULL,
    timezone        VARCHAR(64)     NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    locale          VARCHAR(10)     NOT NULL DEFAULT 'vi',
    role            ENUM('USER', 'ADMIN')      NOT NULL DEFAULT 'USER',
    plan            ENUM('FREE', 'PREMIUM')    NOT NULL DEFAULT 'FREE',
    plan_expires_at DATETIME(6)     NULL,
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    status          ENUM('ACTIVE', 'BLOCKED')  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    deleted_at      DATETIME(6)     NULL,

    PRIMARY KEY (id),
    -- A UNIQUE key is itself an index, so it doubles as idx_users_email from design.md 5.2
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_plan (plan)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
