package com.example.marketplace.settings;

import com.example.marketplace.outbox.OutboxEntity;
import com.example.marketplace.outbox.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Бизнес-логика настроек. Сюда позже добавится проверка, что текущий пользователь
 * имеет доступ к accountId (Auth и tenancy).
 */
@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final OutboxRepository outboxRepository;

    public SettingsService(SettingsRepository settingsRepository, OutboxRepository outboxRepository) {
        this.settingsRepository = settingsRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public Settings get(String accountId) {
        return settingsRepository.findById(accountId)
                .map(SettingsEntity::toSettings)
                .orElseThrow(() -> new SettingsNotFoundException(accountId));
    }

    /**
     * Одна транзакция: новая версия в settings и запись в outbox.
     * Если запись в outbox упадёт, изменение settings откатится вместе с ней.
     */
    @Transactional
    public Settings update(String accountId, SettingsValues values) {
        SettingsEntity entity = settingsRepository.findById(accountId)
                .orElseGet(() -> new SettingsEntity(accountId));
        Long previousVersion = entity.getVersion();

        entity.setValues(values);
        // INSERT/UPDATE выполняется в базе прямо сейчас (но ещё не закоммичен),
        // после этого в entity актуальная version
        settingsRepository.saveAndFlush(entity);

        // Если значения не изменились, Hibernate не делает UPDATE и version остаётся прежней:
        // новой версии нет, доставлять нечего
        if (!entity.getVersion().equals(previousVersion)) {
            outboxRepository.save(new OutboxEntity(accountId, entity.getVersion(), values));
        }
        return entity.toSettings();
    }
}
