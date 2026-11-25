-- Demo data for local runs only (test classpath, 'demo' profile). Covers every plan rule boundary.
INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic',   30,  1000.00),    -- no interest yet (first 30 days)
    (2, 'basic',   45,  1234567.00), -- 1% p.a.
    (3, 'student', 365, 5000.00),    -- 3% p.a., last day with interest
    (4, 'student', 366, 5000.00),    -- no interest after one year
    (5, 'premium', 45,  10000.00),   -- no interest yet (starts after 45 days)
    (6, 'premium', 46,  10000.00),   -- 5% p.a.
    (7, 'gold',    100, 750.00);     -- unknown plan type: listed, never earns interest

INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 2, 1000.00, '2026-01-15'),
    (2, 2, 250.50,  '2026-02-15'),
    (3, 6, 500.00,  '2026-03-01');
