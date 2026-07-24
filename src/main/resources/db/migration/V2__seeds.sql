INSERT INTO sellers (id, name)
VALUES
('11111111-1111-1111-1111-111111111111', 'João Silva'),

('22222222-2222-2222-2222-222222222222', 'Maria Souza'),

('33333333-3333-3333-3333-333333333333', 'Pedro Oliveira');

INSERT INTO budgets (
    id,
    seller_id,
    competence,
    limit_amount,
    balance
)
VALUES

(
'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
'11111111-1111-1111-1111-111111111111',
'2026-07-01',
1000,
850
),

(
'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
'22222222-2222-2222-2222-222222222222',
'2026-07-01',
1000,
50
),

(
'cccccccc-cccc-cccc-cccc-cccccccccccc',
'33333333-3333-3333-3333-333333333333',
'2026-07-01',
1000,
0
);


INSERT INTO sales_order (
    id,
    seller_id,
    discount,
    status
)
VALUES

(
'aaaaaaaa-1111-1111-1111-111111111111',
'11111111-1111-1111-1111-111111111111',
100,
'OPEN'
),

(
'aaaaaaaa-2222-2222-2222-222222222222',
'11111111-1111-1111-1111-111111111111',
200,
'OPEN'
),

(
'aaaaaaaa-3333-3333-3333-333333333333',
'22222222-2222-2222-2222-222222222222',
80,
'OPEN'
),

(
'aaaaaaaa-4444-4444-4444-444444444444',
'33333333-3333-3333-3333-333333333333',
20,
'OPEN'
),

(
'aaaaaaaa-5555-5555-5555-555555555555',
'11111111-1111-1111-1111-111111111111',
150,
'CLOSED'
);


INSERT INTO budget_movements
(
id,
budget_id,
order_id,
movement_type,
amount
)
VALUES
(
'dddddddd-dddd-dddd-dddd-dddddddddddd',
'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
'aaaaaaaa-5555-5555-5555-555555555555',
'CONSUMPTION',
150
);