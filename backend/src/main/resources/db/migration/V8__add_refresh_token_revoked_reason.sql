-- V8: why a refresh token was revoked (design.md 5.2, 6.1). Mirrored by model/RefreshToken.java and
-- model/enums/RevokedReason.java (ddl-auto=validate).
-- Only a token revoked by rotation is a sign of theft when it is presented again; a token revoked by a logout
-- or a password reset is just a stale cookie on another device. Rows revoked before this migration keep NULL
-- and are handled like ROTATED, the behaviour they were revoked under.

ALTER TABLE refresh_tokens
    ADD COLUMN revoked_reason ENUM('ROTATED', 'LOGOUT', 'PASSWORD_RESET', 'BLOCKED', 'EXPIRED', 'REUSE_DETECTED') NULL
        AFTER revoked_at;
