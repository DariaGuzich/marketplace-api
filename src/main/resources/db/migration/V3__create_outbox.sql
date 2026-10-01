-- Outbox: каждое изменение настроек записывается сюда в той же транзакции, что и в settings.
-- Позже Config publisher будет читать записи со статусом NEW и доставлять их в Serving.
CREATE TABLE outbox (
    id         BIGSERIAL    PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    version    BIGINT       NOT NULL,
    payload    JSONB        NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (account_id, version)
);
