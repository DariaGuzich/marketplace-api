package com.example.marketplace.settings;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceTest {

    private final SettingsService service = new SettingsService(new InMemorySettingsRepository());

    @Test
    void getThrowsWhenSettingsNotSaved() {
        assertThatThrownBy(() -> service.get("acc-1"))
                .isInstanceOf(SettingsNotFoundException.class);
    }

    @Test
    void getReturnsSavedSettings() {
        Settings settings = new Settings(new BigDecimal("1.50"), "USD", List.of("bad.com"));

        service.update("acc-1", settings);

        assertThat(service.get("acc-1")).isEqualTo(settings);
    }

    @Test
    void settingsAreStoredPerAccount() {
        service.update("acc-1", new Settings(new BigDecimal("1.50"), "USD", List.of()));

        assertThatThrownBy(() -> service.get("acc-2"))
                .isInstanceOf(SettingsNotFoundException.class);
    }
}
