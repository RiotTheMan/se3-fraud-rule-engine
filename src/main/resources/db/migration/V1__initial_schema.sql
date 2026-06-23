CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL UNIQUE,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_customer_id ON customers (customer_id);

CREATE TABLE transactions (
    id                 BIGSERIAL PRIMARY KEY,
    transaction_id     VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key    VARCHAR(255) NOT NULL UNIQUE,
    customer_id        BIGINT NOT NULL REFERENCES customers (id),
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    merchant_name      VARCHAR(255),
    merchant_category  VARCHAR(100),
    country_code       VARCHAR(3),
    latitude           DOUBLE PRECISION,
    longitude          DOUBLE PRECISION,
    transaction_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_customer_created ON transactions (customer_id, created_at DESC);
CREATE INDEX idx_transactions_transaction_id    ON transactions (transaction_id);
CREATE INDEX idx_transactions_transaction_at    ON transactions (transaction_at DESC);

CREATE TABLE fraud_rules (
    id            BIGSERIAL PRIMARY KEY,
    rule_name     VARCHAR(100) NOT NULL UNIQUE,
    description   VARCHAR(500),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    severity      VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    threshold     NUMERIC(19, 4),
    window_minutes INTEGER,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_rules_rule_name ON fraud_rules (rule_name);
CREATE INDEX idx_fraud_rules_enabled   ON fraud_rules (enabled);

CREATE TABLE fraud_flags (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL REFERENCES transactions (id),
    customer_id     BIGINT NOT NULL REFERENCES customers (id),
    rule_name       VARCHAR(100) NOT NULL,
    severity        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reason          VARCHAR(1000),
    rule_details    TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_flags_transaction_id        ON fraud_flags (transaction_id);
CREATE INDEX idx_fraud_flags_customer_status       ON fraud_flags (customer_id, status);
CREATE INDEX idx_fraud_flags_severity_created      ON fraud_flags (severity, created_at DESC);
CREATE INDEX idx_fraud_flags_status                ON fraud_flags (status);
