-- Migration V2: users who can log in to the API.
-- We store a BCrypt HASH of the password, never the password itself.

CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    -- UNIQUE: the database itself refuses two users with the same name
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('OFFICER', 'ADMIN')),
    created_at     TIMESTAMPTZ  NOT NULL
);