CREATE TABLE settings (
    account_id      VARCHAR(255) PRIMARY KEY,
    floor_price     NUMERIC      NOT NULL,
    currency        VARCHAR(255) NOT NULL,
    blocked_domains TEXT[]       NOT NULL
);
