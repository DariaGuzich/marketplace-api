package com.example.marketplace.idempotency;

import com.example.marketplace.settings.Settings;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Строка таблицы idempotency_keys: ключ запроса и ответ, который на него был отдан. */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private Settings response;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, Settings response) {
        this.idempotencyKey = idempotencyKey;
        this.response = response;
    }

    public Settings getResponse() {
        return response;
    }
}
