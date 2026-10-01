package com.example.marketplace.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Настройки в ответе API: значения и версия.
 * version увеличивается при каждом изменении настроек; первая сохранённая версия — 0.
 */
public record Settings(
        @JsonProperty("floor_price") @NotNull BigDecimal floorPrice,
        @JsonProperty("currency") @NotNull String currency,
        @JsonProperty("blocked_domains") @NotNull List<String> blockedDomains,
        @JsonProperty("version") @NotNull Long version) {
}
