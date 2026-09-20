CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    avatar_preset_id BIGINT,
    blocked_until TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS school_classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS class_memberships (
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, school_class_id)
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    total_pages INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_page INTEGER
);

CREATE TABLE IF NOT EXISTS reading_sessions (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    session_date TIMESTAMP WITH TIME ZONE NOT NULL,
    minutes INTEGER NOT NULL,
    pages_from INTEGER NOT NULL,
    pages_to INTEGER NOT NULL,
    note VARCHAR(1000),
    points_awarded NUMERIC(10,2),
    marked_finished BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(1000) NOT NULL,
    default_points NUMERIC(10,2),
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz_responses (
    id BIGSERIAL PRIMARY KEY,
    reading_session_id BIGINT NOT NULL,
    quiz_question_id BIGINT NOT NULL,
    answer_text VARCHAR(2000),
    points_awarded NUMERIC(10,2)
);

CREATE TABLE IF NOT EXISTS shop_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price_points NUMERIC(10,2) NOT NULL,
    cooldown_days INTEGER,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_item_id BIGINT NOT NULL,
    price_points NUMERIC(10,2) NOT NULL,
    purchased_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_adjustments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(255) PRIMARY KEY,
    setting_value VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS featured_posts (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(1000) NOT NULL,
    image_blob_id BIGINT,
    link_url VARCHAR(500),
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS image_blobs (
    id BIGSERIAL PRIMARY KEY,
    mime_type VARCHAR(100) NOT NULL,
    data BYTEA NOT NULL
);

CREATE TABLE IF NOT EXISTS avatar_presets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_blob_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    payload_json VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('points_per_page', '0'),
    ('points_per_minute', '0.1'),
    ('quiz_show_probability', '0.6'),
    ('audit_retention_days', '90');
