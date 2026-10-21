-- ==============================================================
-- Finance System Manager - Relational Database Schema
-- Compatible with MySQL 8.0+ and embedded H2 MySQL mode
-- ==============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    invitation_code VARCHAR(50) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS family_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_user_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_by VARCHAR(20) NOT NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_family_student FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_parent_student UNIQUE (parent_user_id, student_user_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_student_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_method VARCHAR(50),
    source VARCHAR(20) DEFAULT 'MANUAL',
    reference_hash VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_owner FOREIGN KEY (owner_student_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tx_owner ON transactions(owner_student_id);
CREATE INDEX idx_tx_date ON transactions(transaction_date);
CREATE INDEX idx_tx_cat ON transactions(category);

CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    limit_amount DECIMAL(12,2) NOT NULL,
    warning_threshold DECIMAL(5,2) DEFAULT 80.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_student FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_cat_month UNIQUE (student_user_id, category, period_month)
);

CREATE TABLE IF NOT EXISTS savings_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(12,2) NOT NULL,
    saved_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    target_date DATE,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goal_student FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS investments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_user_id BIGINT NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    asset_name VARCHAR(100) NOT NULL,
    invested_amount DECIMAL(12,2) NOT NULL,
    current_value DECIMAL(12,2) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_student FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sip_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_user_id BIGINT NOT NULL,
    fund_name VARCHAR(100) NOT NULL,
    monthly_amount DECIMAL(12,2) NOT NULL,
    start_date DATE NOT NULL,
    due_day_of_month INT NOT NULL DEFAULT 5,
    payment_status VARCHAR(20) DEFAULT 'ACTIVE',
    assumed_return_rate DECIMAL(5,2) DEFAULT 12.00,
    duration_months INT NOT NULL DEFAULT 36,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sip_student FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS statement_imports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    total_rows INT DEFAULT 0,
    imported_rows INT DEFAULT 0,
    duplicate_rows INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(50),
    metadata TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
