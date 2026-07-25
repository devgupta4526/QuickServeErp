-- V8__whatsapp_notifications.sql
-- WhatsApp configuration, message logs, webhook events

CREATE TABLE whatsapp_configs (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id                UUID NOT NULL REFERENCES businesses(id),
    phone_number_id            VARCHAR(50),
    waba_id                    VARCHAR(50),
    access_token_encrypted     TEXT,
    verified_number            VARCHAR(20),
    is_active                  BOOLEAN NOT NULL DEFAULT false,
    notify_order_confirmation  BOOLEAN NOT NULL DEFAULT true,
    notify_order_ready         BOOLEAN NOT NULL DEFAULT true,
    notify_invoice_delivery    BOOLEAN NOT NULL DEFAULT true,
    notify_low_stock           BOOLEAN NOT NULL DEFAULT true,
    notify_daily_summary       BOOLEAN NOT NULL DEFAULT true,
    notify_payslip             BOOLEAN NOT NULL DEFAULT true,
    notify_loyalty_update      BOOLEAN NOT NULL DEFAULT true,
    notify_reservation_confirm BOOLEAN NOT NULL DEFAULT true,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id)
);

CREATE TABLE whatsapp_message_log (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id        UUID NOT NULL,
    to_number          VARCHAR(20) NOT NULL,
    template_name      VARCHAR(100) NOT NULL,
    template_variables JSONB,
    status             VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, READ, FAILED, PERMANENTLY_FAILED
    wamid              VARCHAR(100),
    error_message      TEXT,
    retry_count        INT NOT NULL DEFAULT 0,
    sent_at            TIMESTAMPTZ,
    delivered_at       TIMESTAMPTZ,
    read_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wa_log_business ON whatsapp_message_log(business_id);
CREATE INDEX idx_wa_log_status   ON whatsapp_message_log(status);
CREATE INDEX idx_wa_log_wamid    ON whatsapp_message_log(wamid);

CREATE TABLE whatsapp_webhook_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(50),
    wamid       VARCHAR(100),
    payload     JSONB NOT NULL,
    processed   BOOLEAN NOT NULL DEFAULT false,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wa_webhook_processed ON whatsapp_webhook_events(processed);
