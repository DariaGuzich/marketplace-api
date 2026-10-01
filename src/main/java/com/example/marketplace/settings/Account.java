package com.example.marketplace.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** Элемент списка аккаунтов. Отдельной таблицы аккаунтов нет: аккаунт — это тот, у кого сохранены настройки. */
public record Account(@JsonProperty("account_id") @NotNull String accountId) {
}
