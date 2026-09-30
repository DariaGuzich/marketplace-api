package com.example.marketplace.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Настройки маркетплейса для аккаунта. Одна модель и для запроса PUT, и для ответа.
 * <p>
 * {@code @JsonProperty} задаёт имя поля в JSON (snake_case). Эту аннотацию читают и Spring
 * при сериализации (Jackson 3), и springdoc при генерации openapi.json (Jackson 2),
 * поэтому имя поля в ответе и в спецификации берётся из одного места.
 * <p>
 * {@code @NotNull} делает поле обязательным при валидации и помечает его как required в openapi.json.
 */
public record Settings(
        @JsonProperty("min_price") @NotNull BigDecimal floorPrice,
        @JsonProperty("currency") @NotNull String currency,
        @JsonProperty("blocked_domains") @NotNull List<String> blockedDomains) {
}
