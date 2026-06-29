--liquibase formatted sql

--changeset cinema-system:create-idempotency-table
--rollback DROP TABLE IF EXISTS idempotency_key;
CREATE TABLE IF NOT EXISTS idempotency_key
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    response    TEXT      NOT NULL,
    expiry_date TIMESTAMP NOT NULL
);

--changeset cinema-system:create-tickets-table
--rollback DROP TABLE IF EXISTS ticket;
CREATE TABLE IF NOT EXISTS ticket
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    payment_id     UUID           NOT NULL,
    customer_email TEXT           NOT NULL,
    payment_method TEXT           NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    payment_date   TIMESTAMP      NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    booking_id     UUID           NOT NULL,

    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255)

        CONSTRAINT chk_ticket_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);