-- Seed transactions for demo/teaching purposes
INSERT INTO transactions (transaction_ref, account_number, amount, type, status, description, balance_after, created_at)
VALUES ('TXN-SEED000001', 'ACC0000000001', 1000.00, 'DEPOSIT', 'COMPLETED', 'Initial deposit', 1000.00, CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_ref, account_number, amount, type, status, description, balance_after, created_at)
VALUES ('TXN-SEED000002', 'ACC0000000001', 200.00, 'WITHDRAWAL', 'COMPLETED', 'ATM withdrawal', 800.00, CURRENT_TIMESTAMP);

INSERT INTO transactions (transaction_ref, account_number, target_account_number, amount, type, status, description, balance_after, created_at)
VALUES ('TXN-SEED000003', 'ACC0000000001', 'ACC0000000002', 150.00, 'TRANSFER', 'COMPLETED', 'Rent payment', 650.00, CURRENT_TIMESTAMP);
