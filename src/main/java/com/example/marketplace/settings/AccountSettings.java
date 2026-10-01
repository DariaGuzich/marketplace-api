package com.example.marketplace.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Настройки вместе с account_id: элемент ответа batch-запроса GET /settings?account_ids=... */
public record AccountSettings(
        @JsonProperty("account_id") @NotNull String accountId,
        @JsonProperty("floor_price") @NotNull BigDecimal floorPrice,
        @JsonProperty("currency") @NotNull String currency,
        @JsonProperty("blocked_domains") @NotNull List<String> blockedDomains,
        @JsonProperty("version") @NotNull Long version) {
}
