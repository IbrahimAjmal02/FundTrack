CREATE TABLE client (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    subaccount_number VARCHAR(20)  NOT NULL UNIQUE
);

CREATE TABLE fund (
    id     BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10)  NOT NULL UNIQUE,
    name   VARCHAR(150) NOT NULL
);

CREATE TABLE nav_price (
    id         BIGSERIAL PRIMARY KEY,
    fund_id    BIGINT         NOT NULL REFERENCES fund (id),
    price_date DATE           NOT NULL,
    nav        NUMERIC(12, 4) NOT NULL CHECK (nav > 0),
    UNIQUE (fund_id, price_date)
);

CREATE TABLE fund_order (
    id            BIGSERIAL PRIMARY KEY,
    client_id     BIGINT         NOT NULL REFERENCES client (id),
    fund_id       BIGINT         NOT NULL REFERENCES fund (id),
    order_type    VARCHAR(4)     NOT NULL CHECK (order_type IN ('BUY', 'SELL')),
    amount        NUMERIC(14, 2),
    units         NUMERIC(16, 4),
    order_date    DATE           NOT NULL,
    status        VARCHAR(10)    NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'SETTLED', 'REJECTED')),
    price_used    NUMERIC(12, 4),
    units_settled NUMERIC(16, 4),
    created_at    TIMESTAMP      NOT NULL DEFAULT now(),
    CHECK (
        (order_type = 'BUY'  AND amount IS NOT NULL AND amount > 0) OR
        (order_type = 'SELL' AND units  IS NOT NULL AND units  > 0)
    )
);

CREATE INDEX idx_fund_order_status_date ON fund_order (status, order_date);
CREATE INDEX idx_fund_order_client ON fund_order (client_id);
