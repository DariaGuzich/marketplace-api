package com.example.marketplace.outbox;

import com.example.marketplace.settings.SettingsValues;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Строка таблицы outbox: версия конфига аккаунта, которую нужно доставить в Serving. */
@Entity
@Table(name = "outbox")
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;

    private Long version;

    // Хранится в колонке JSONB как {"floor_price": ..., "currency": ..., "blocked_domains": [...]}
    @JdbcTypeCode(SqlTypes.JSON)
    private SettingsValues payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    protected OutboxEntity() {
    }

    public OutboxEntity(String accountId, Long version, SettingsValues payload) {
        this.accountId = accountId;
        this.version = version;
        this.payload = payload;
        this.status = OutboxStatus.NEW;
    }

    public String getAccountId() {
        return accountId;
    }

    public Long getVersion() {
        return version;
    }

    public SettingsValues getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }
}
