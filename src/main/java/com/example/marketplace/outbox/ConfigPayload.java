package com.example.marketplace.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** Данные конфига в outbox (колонка payload, JSONB). */
public record ConfigPayload(
        @JsonProperty("floor_price") BigDecimal floorPrice,
        @JsonProperty("currency") String currency,
        @JsonProperty("blocked_domains") List<String> blockedDomains) {
}
