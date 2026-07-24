CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE sellers (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    competence DATE NOT NULL,
    limit_amount NUMERIC(12,2) NOT NULL,
    balance NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_budget_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(id),

    CONSTRAINT uk_budget_seller_competence
        UNIQUE (seller_id, competence),

    CONSTRAINT chk_budget_limit
        CHECK (limit_amount >= 0),

    CONSTRAINT chk_budget_balance
        CHECK (balance >= 0)
);

CREATE TABLE sales_order (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    discount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_at TIMESTAMP,

    CONSTRAINT fk_order_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(id),

    CONSTRAINT chk_discount
        CHECK (discount >= 0)
);

CREATE TABLE budget_movements (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL,
    order_id UUID NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movement_budget
        FOREIGN KEY (budget_id)
        REFERENCES budgets(id),

    CONSTRAINT fk_movement_order
        FOREIGN KEY (order_id)
        REFERENCES sales_order(id),

    CONSTRAINT chk_amount
        CHECK (amount > 0),

    CONSTRAINT chk_movement_type
        CHECK (movement_type IN ('CONSUMPTION', 'REVERSAL')),

    CONSTRAINT uk_order_movement_type
        UNIQUE (order_id, movement_type)
);

CREATE INDEX idx_budget_seller
    ON budgets(seller_id);

CREATE INDEX idx_order_seller
    ON sales_order(seller_id);

CREATE INDEX idx_budget_movement_budget
    ON budget_movements(budget_id);

CREATE INDEX idx_budget_movement_order
    ON budget_movements(order_id);