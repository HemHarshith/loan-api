-- Migration V1: creates the loan_application table.
-- Flyway runs each V-file once, in number order, and records it,
-- so the database structure is versioned like code.

CREATE TABLE loan_application (
    -- BIGSERIAL = auto-increasing number, filled in by the database
    id               BIGSERIAL PRIMARY KEY,
    applicant_name   VARCHAR(100)   NOT NULL,
    age              INTEGER        NOT NULL CHECK (age BETWEEN 18 AND 100),
    -- NUMERIC is exact (no rounding errors), used for money
    loan_amount      NUMERIC(15, 2) NOT NULL,
    duration_months  INTEGER        NOT NULL,
    purpose          VARCHAR(100)   NOT NULL,
    -- status stored as text: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
    status           VARCHAR(20)    NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL
);