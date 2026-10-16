CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub TEXT NOT NULL,
    name VARCHAR(120) NOT NULL,
    email TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_customer_sub UNIQUE (sub),
    CONSTRAINT chk_customer_name CHECK (char_length(btrim(name)) BETWEEN 1 AND 120)
);

CREATE UNIQUE INDEX uk_customer_email_canonical
    ON customer (lower(btrim(email)));
