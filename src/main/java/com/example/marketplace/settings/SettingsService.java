package com.example.marketplace.settings;

import org.springframework.stereotype.Service;

/**
 * Бизнес-логика настроек. Сюда позже добавятся:
 * - проверка, что текущий пользователь имеет доступ к accountId (Auth и tenancy);
 * - публикация изменённых настроек в Config publisher после update.
 */
@Service
public class SettingsService {

    private final SettingsRepository repository;

    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    public Settings get(String accountId) {
        return repository.findByAccountId(accountId)
                .orElseThrow(() -> new SettingsNotFoundException(accountId));
    }

    public Settings update(String accountId, Settings settings) {
        return repository.save(accountId, settings);
    }
}
