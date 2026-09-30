package com.example.marketplace.settings;

import java.util.Optional;

/**
 * Слой хранения. Сейчас единственная реализация хранит данные в памяти;
 * позже появится реализация на PostgreSQL, а остальной код не изменится.
 */
public interface SettingsRepository {

    Optional<Settings> findByAccountId(String accountId);

    Settings save(String accountId, Settings settings);
}
