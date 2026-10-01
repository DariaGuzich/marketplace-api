-- Ключи идемпотентности: для каждого ключа хранится ответ, который получил клиент.
-- Повторный запрос с тем же ключом получает этот ответ, а операция второй раз не выполняется.
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    response        JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
