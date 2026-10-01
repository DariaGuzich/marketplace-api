-- Миграция, которая меняет существующие данные:
-- у всех строк, созданных до неё, version становится 0 (значение по умолчанию).
ALTER TABLE settings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
