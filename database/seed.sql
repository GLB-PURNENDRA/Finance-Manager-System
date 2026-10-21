-- ==============================================================
-- Finance System Manager - Safe Sample Seed Data
-- ==============================================================

-- Password for both demo accounts is: Password123!
-- (BCrypt hash: $2a$10$wK1Vq8l7d1TzM6.6y4jG0OCYk8dO3m2b5u2N5n5f4u6c3d2e1a)

INSERT INTO users (id, email, password_hash, display_name, role, invitation_code, status) VALUES
(1, 'student@university.edu', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8/xQ4N2m4tHkYQ5l3M2N.tF2nQZc/K', 'Alex Chen (Student)', 'STUDENT', 'STU-ALEX-8842', 'ACTIVE'),
(2, 'parent@family.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8/xQ4N2m4tHkYQ5l3M2N.tF2nQZc/K', 'Sarah Chen (Parent)', 'PARENT', NULL, 'ACTIVE');

-- Active link between Parent Sarah and Student Alex
INSERT INTO family_links (id, parent_user_id, student_user_id, status, requested_by, approved_at) VALUES
(1, 2, 1, 'ACTIVE', 'PARENT', CURRENT_TIMESTAMP);

-- Realistic student transactions
INSERT INTO transactions (id, owner_student_id, type, amount, category, description, transaction_date, payment_method, source) VALUES
(1, 1, 'INCOME', 15000.00, 'Allowance', 'Monthly Family Allowance', '2026-10-01', 'Bank Transfer', 'MANUAL'),
(2, 1, 'INCOME', 5000.00, 'Stipend', 'Research Lab Assistant Stipend', '2026-10-05', 'Direct Deposit', 'MANUAL'),
(3, 1, 'EXPENSE', 6500.00, 'Hostel & Rent', 'University Hostel Room Rent', '2026-10-02', 'Net Banking', 'MANUAL'),
(4, 1, 'EXPENSE', 3200.00, 'Food & Mess', 'Campus Monthly Mess Dues', '2026-10-03', 'UPI', 'MANUAL'),
(5, 1, 'EXPENSE', 1200.00, 'Books & Supplies', 'Data Structures & Algorithms Textbook', '2026-10-06', 'Card', 'MANUAL'),
(6, 1, 'EXPENSE', 450.00, 'Transport', 'Monthly City Bus Student Pass', '2026-10-07', 'Cash', 'MANUAL'),
(7, 1, 'EXPENSE', 199.00, 'Subscriptions', 'Music & Code Editor Subscription', '2026-10-08', 'UPI', 'MANUAL'),
(8, 1, 'EXPENSE', 45.00, 'Food & Mess', 'Campus Canteen Tea & Biscuit', '2026-10-09', 'UPI', 'MANUAL'),
(9, 1, 'EXPENSE', 55.00, 'Food & Mess', 'Campus Canteen Evening Snack', '2026-10-10', 'UPI', 'MANUAL'),
(10, 1, 'EXPENSE', 50.00, 'Food & Mess', 'Campus Canteen Tea & Biscuit', '2026-10-12', 'UPI', 'MANUAL'),
(11, 1, 'EXPENSE', 850.00, 'Personal', 'Weekend Movie & Dinner', '2026-10-14', 'UPI', 'MANUAL');

-- Monthly category budgets for current period
INSERT INTO budgets (id, student_user_id, category, period_month, limit_amount, warning_threshold) VALUES
(1, 1, 'Food & Mess', '2026-10', 4500.00, 80.00),
(2, 1, 'Books & Supplies', '2026-10', 2000.00, 80.00),
(3, 1, 'Personal', '2026-10', 1500.00, 80.00);

-- Savings goals
INSERT INTO savings_goals (id, student_user_id, name, target_amount, saved_amount, target_date, status) VALUES
(1, 1, 'Semester Emergency Fund', 20000.00, 12500.00, '2026-12-31', 'IN_PROGRESS'),
(2, 1, 'New Coding Laptop Fund', 60000.00, 18000.00, '2027-06-30', 'IN_PROGRESS');

-- Investment portfolio
INSERT INTO investments (id, student_user_id, asset_type, asset_name, invested_amount, current_value) VALUES
(1, 1, 'MUTUAL_FUNDS', 'Nifty 50 Index Fund Direct Growth', 15000.00, 16850.00),
(2, 1, 'STOCKS', 'Clean Energy Tech ETF', 5000.00, 5420.00);

-- SIP tracker
INSERT INTO sip_records (id, student_user_id, fund_name, monthly_amount, start_date, due_day_of_month, payment_status, assumed_return_rate, duration_months) VALUES
(1, 1, 'Flexi-Cap Long Term Growth SIP', 1500.00, '2026-01-05', 5, 'ACTIVE', 12.50, 36);
