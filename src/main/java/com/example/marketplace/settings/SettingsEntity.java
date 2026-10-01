package com.example.marketplace.settings;

import com.example.marketplace.outbox.ConfigPayload;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

/** Строка таблицы settings (схема создаётся миграциями Flyway, см. db/migration). */
@Entity
@Table(name = "settings")
public class SettingsEntity {

    @Id
    private String accountId;

    private BigDecimal floorPrice;

    private String currency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> blockedDomains;

    // Hibernate сам увеличивает version при каждом UPDATE, если данные изменились.
    // У новой записи version = null, при INSERT Hibernate ставит 0.
    @Version
    private Long version;

    protected SettingsEntity() {
    }

    public SettingsEntity(String accountId) {
        this.accountId = accountId;
    }

    public void setValues(ConfigPayload values) {
        this.floorPrice = values.floorPrice();
        this.currency = values.currency();
        this.blockedDomains = values.blockedDomains();
    }

    public ConfigPayload toPayload() {
        return new ConfigPayload(floorPrice, currency, blockedDomains);
    }

    public String getAccountId() {
        return accountId;
    }

    public Long getVersion() {
        return version;
    }

    public Settings toSettings() {
        return new Settings(floorPrice, currency, blockedDomains, version);
    }
}
