package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import com.example.marketplace.outbox.OutboxEntity;
import com.example.marketplace.outbox.OutboxRepository;
import com.example.marketplace.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Сервис с настоящим PostgreSQL (Testcontainers): версии и записи в outbox.
 * Каждый тест работает со своим accountId, поэтому тестам не мешают данные друг друга.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SettingsServiceTest {

    @Autowired
    SettingsService service;

    @Autowired
    OutboxRepository outboxRepository;

    @Test
    void getThrowsWhenSettingsNotSaved() {
        assertThatThrownBy(() -> service.get("svc-unknown"))
                .isInstanceOf(SettingsNotFoundException.class);
    }

    @Test
    void firstUpdateCreatesVersion0AndOutboxRecord() {
        SettingsValues values = new SettingsValues(new BigDecimal("1.5"), "USD", List.of("bad.com"));

        Settings saved = service.update("svc-first", values);

        assertThat(saved.version()).isEqualTo(0L);
        assertThat(outboxFor("svc-first"))
                .extracting(OutboxEntity::getVersion, OutboxEntity::getPayload, OutboxEntity::getStatus)
                .containsExactly(tuple(0L, values, OutboxStatus.NEW));
    }

    @Test
    void eachChangeIncrementsVersionAndAddsOutboxRecord() {
        service.update("svc-change", new SettingsValues(new BigDecimal("1.5"), "USD", List.of()));
        Settings saved = service.update("svc-change", new SettingsValues(new BigDecimal("2.0"), "USD", List.of()));

        assertThat(saved.version()).isEqualTo(1L);
        assertThat(service.get("svc-change").version()).isEqualTo(1L);
        assertThat(outboxFor("svc-change")).extracting(OutboxEntity::getVersion).containsExactly(0L, 1L);
    }

    @Test
    void sameValuesDoNotCreateNewVersion() {
        SettingsValues values = new SettingsValues(new BigDecimal("1.5"), "USD", List.of("bad.com"));
        service.update("svc-same", values);

        Settings saved = service.update("svc-same", values);

        assertThat(saved.version()).isEqualTo(0L);
        assertThat(outboxFor("svc-same")).hasSize(1);
    }

    private List<OutboxEntity> outboxFor(String accountId) {
        return outboxRepository.findAll().stream()
                .filter(record -> record.getAccountId().equals(accountId))
                .toList();
    }
}
