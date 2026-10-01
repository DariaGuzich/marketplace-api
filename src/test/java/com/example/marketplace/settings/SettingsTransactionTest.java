package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import com.example.marketplace.outbox.OutboxEntity;
import com.example.marketplace.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Транзакция settings + outbox: если вторая запись (outbox) падает, первая (settings) не сохраняется.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SettingsTransactionTest {

    @Autowired
    SettingsService service;

    @Autowired
    SettingsRepository settingsRepository;

    // Вместо настоящего репозитория outbox — мок, который всегда падает при записи
    @MockitoBean
    OutboxRepository outboxRepository;

    @Test
    void settingsAreRolledBackWhenOutboxWriteFails() {
        when(outboxRepository.save(any(OutboxEntity.class)))
                .thenThrow(new IllegalStateException("outbox is down"));
        SettingsValues values = new SettingsValues(new BigDecimal("1.5"), "USD", List.of());

        // К моменту падения INSERT в settings уже выполнен в базе (saveAndFlush в сервисе),
        // но транзакция не закоммичена, поэтому он откатывается
        assertThatThrownBy(() -> service.update("tx-account", values))
                .hasMessage("outbox is down");

        assertThat(settingsRepository.findById("tx-account")).isEmpty();
    }
}
