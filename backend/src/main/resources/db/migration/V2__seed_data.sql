INSERT INTO client (name, subaccount_number) VALUES
    ('Sarah Chen',  'SA-100001'),
    ('Kumar Patel', 'SA-100002'),
    ('Maria Lopez', 'SA-100003');

INSERT INTO fund (ticker, name) VALUES
    ('MPL100', 'Maple Balanced Growth Fund'),
    ('NRT200', 'Northern Canadian Equity Fund'),
    ('BND300', 'Harbour Bond Income Fund');

INSERT INTO nav_price (fund_id, price_date, nav) VALUES
    ((SELECT id FROM fund WHERE ticker = 'MPL100'), DATE '2026-11-02', 12.4500),
    ((SELECT id FROM fund WHERE ticker = 'NRT200'), DATE '2026-11-02', 25.1000),
    ((SELECT id FROM fund WHERE ticker = 'BND300'), DATE '2026-11-02', 10.0200);
