package com.example.marketplace.settings;

import com.example.marketplace.idempotency.IdempotencyRecord;
import com.example.marketplace.idempotency.IdempotencyRepository;
import com.example.marketplace.outbox.ConfigPayload;
import com.example.marketplace.outbox.OutboxEntity;
import com.example.marketplace.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Бизнес-логика настроек. Сюда позже добавится проверка, что текущий пользователь
 * имеет доступ к accountId (Auth и tenancy).
 */
@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final boolean optimisticLockingEnabled;

    public SettingsService(SettingsRepository settingsRepository,
                           OutboxRepository outboxRepository,
                           IdempotencyRepository idempotencyRepository,
                           @Value("${marketplace.optimistic-locking.enabled}") boolean optimisticLockingEnabled) {
        this.settingsRepository = settingsRepository;
        this.outboxRepository = outboxRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.optimisticLockingEnabled = optimisticLockingEnabled;
    }

    @Transactional(readOnly = true)
    public Settings get(String accountId) {
        return settingsRepository.findById(accountId)
                .map(SettingsEntity::toSettings)
                .orElseThrow(() -> new SettingsNotFoundException(accountId));
    }

    /**
     * PUT: полная замена настроек.
     * Если клиент передал version, которую видел, и она не совпадает с текущей — 409 (optimistic locking).
     */
    @Transactional
    public Settings update(String accountId, SettingsValues request) {
        SettingsEntity entity = settingsRepository.findById(accountId)
                .orElseGet(() -> new SettingsEntity(accountId));

        if (optimisticLockingEnabled && request.version() != null && !request.version().equals(entity.getVersion())) {
            throw new VersionConflictException(accountId, request.version(), entity.getVersion());
        }

        return save(entity, new ConfigPayload(request.floorPrice(), request.currency(), request.blockedDomains()));
    }

    /**
     * POST: добавить один домен. Операция неидемпотентна: два одинаковых запроса добавят домен дважды.
     * С Idempotency-Key повторный запрос получает сохранённый ответ первого, а домен не добавляется.
     */
    @Transactional
    public Settings addBlockedDomain(String accountId, String domain, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> previous = idempotencyRepository.findById(idempotencyKey);
            if (previous.isPresent()) {
                return previous.get().getResponse();
            }
        }

        SettingsEntity entity = settingsRepository.findById(accountId)
                .orElseThrow(() -> new SettingsNotFoundException(accountId));
        ConfigPayload current = entity.toPayload();
        List<String> domains = new ArrayList<>(current.blockedDomains());
        domains.add(domain);
        Settings result = save(entity, new ConfigPayload(current.floorPrice(), current.currency(), domains));

        // Ключ сохраняется в той же транзакции, что и изменение: либо есть и домен, и ключ, либо ничего
        if (idempotencyKey != null) {
            idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, result));
        }
        return result;
    }

    /**
     * Одна транзакция: новая версия в settings и запись в outbox.
     * Если запись в outbox упадёт, изменение settings откатится вместе с ней.
     */
    private Settings save(SettingsEntity entity, ConfigPayload values) {
        Long previousVersion = entity.getVersion();

        entity.setValues(values);
        // INSERT/UPDATE выполняется в базе прямо сейчас (но ещё не закоммичен),
        // после этого в entity актуальная version.
        // UPDATE идёт с условием WHERE version = <прочитанная> (@Version): если другой запрос успел
        // записать раньше, обновится 0 строк и будет 409 (см. ConflictExceptionHandler)
        settingsRepository.saveAndFlush(entity);

        // Если значения не изменились, Hibernate не делает UPDATE и version остаётся прежней:
        // новой версии нет, доставлять нечего
        if (!entity.getVersion().equals(previousVersion)) {
            outboxRepository.save(new OutboxEntity(entity.getAccountId(), entity.getVersion(), values));
        }
        return entity.toSettings();
    }
}
