-- Seed data for Account Service
-- TEACHING POINT: data.sql runs after Hibernate creates tables (defer-datasource-initialization: true)

-- Create an admin user (password: admin123 → BCrypt hashed)
INSERT INTO users (username, password, email, enabled, created_at)
VALUES ('admin', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk2E7Gy', 'admin@banking.com', true, CURRENT_TIMESTAMP);

-- Create a regular user (password: user123 → BCrypt hashed)
INSERT INTO users (username, password, email, enabled, created_at)
VALUES ('john_doe', '$2a$10$ByIUiNaRfBKSV6urSZRNXuS2oOR5MLd.yLcMSVCWh48yKdLuwuSMC', 'john@example.com', true, CURRENT_TIMESTAMP);

-- Assign roles
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (2, 'ROLE_USER');

-- Seed accounts
INSERT INTO accounts (account_number, owner_name, email, balance, account_type, status, created_at, updated_at)
VALUES ('ACC0000000001', 'John Doe', 'john@example.com', 5000.00, 'CHECKING', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounts (account_number, owner_name, email, balance, account_type, status, created_at, updated_at)
VALUES ('ACC0000000002', 'Jane Smith', 'jane@example.com', 12500.50, 'SAVINGS', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounts (account_number, owner_name, email, balance, account_type, status, created_at, updated_at)
VALUES ('ACC0000000003', 'Acme Corp', 'acme@corp.com', 99999.99, 'BUSINESS', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
