CREATE TABLE customer_idempotency (
    actor_sub TEXT NOT NULL,
    operation VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    response_location TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_customer_idempotency PRIMARY KEY (actor_sub, operation, idempotency_key),
    CONSTRAINT chk_customer_idempotency_expiry CHECK (expires_at > created_at)
);
