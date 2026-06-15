-- =====================================================================
-- FitTracker V1 - Schema initial (Phase 4).
-- Couvre toutes les entites du brief : User, Profile, Exercise,
-- TrainingSession, SessionExercise, Program, Follow, Notification.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------
-- USERS (RGPD soft-delete via deleted_at)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(120) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0
);
-- Unicite de l'email uniquement parmi les comptes actifs (non supprimes).
CREATE UNIQUE INDEX ux_users_email_active
    ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX ix_users_deleted_at ON users (deleted_at);

-- ---------------------------------------------------------------------
-- PROFILES (One-to-One avec User, PK = FK partagee)
-- ---------------------------------------------------------------------
CREATE TABLE profiles (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    height_cm       INTEGER,
    weight_kg       DOUBLE PRECISION,
    goal_weight_kg  DOUBLE PRECISION,
    bio             VARCHAR(500)
);

-- ---------------------------------------------------------------------
-- EXERCISES (referentiel partage)
-- ---------------------------------------------------------------------
CREATE TABLE exercises (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    muscle_group    VARCHAR(60)  NOT NULL,
    unit            VARCHAR(20)  NOT NULL
);
CREATE INDEX ix_exercises_category ON exercises (category);

-- ---------------------------------------------------------------------
-- TRAINING_SESSIONS (N-1 User)
-- ---------------------------------------------------------------------
CREATE TABLE training_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    started_at          TIMESTAMPTZ NOT NULL,
    duration_seconds    INTEGER NOT NULL,
    type                VARCHAR(20) NOT NULL,
    notes               VARCHAR(2000),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_sessions_user_started ON training_sessions (user_id, started_at DESC);
CREATE INDEX ix_sessions_type ON training_sessions (type);

-- ---------------------------------------------------------------------
-- SESSION_EXERCISES (M-N TrainingSession <-> Exercise avec attributs)
-- ---------------------------------------------------------------------
CREATE TABLE session_exercises (
    session_id      UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    exercise_id     UUID NOT NULL REFERENCES exercises(id),
    position        INTEGER NOT NULL,
    sets            INTEGER,
    reps            INTEGER,
    weight_kg       DOUBLE PRECISION,
    distance_m      INTEGER,
    time_seconds    INTEGER,
    PRIMARY KEY (session_id, exercise_id, position)
);
CREATE INDEX ix_session_exercises_exercise ON session_exercises (exercise_id);

-- ---------------------------------------------------------------------
-- PROGRAMS (N-1 User)
-- ---------------------------------------------------------------------
CREATE TABLE programs (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(2000),
    target_metric   VARCHAR(120),
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_programs_user ON programs (user_id);

-- ---------------------------------------------------------------------
-- FOLLOWS (M-N self-referencant User <-> User)
-- ---------------------------------------------------------------------
CREATE TABLE follows (
    follower_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX ix_follows_followee ON follows (followee_id);

-- ---------------------------------------------------------------------
-- NOTIFICATIONS (N-1 User, payload JSONB)
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(40) NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
