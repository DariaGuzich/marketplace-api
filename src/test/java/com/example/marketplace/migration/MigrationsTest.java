package com.example.marketplace.migration;

import com.example.marketplace.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Миграции проверяются напрямую через Flyway, без Spring: только SQL-файлы из db/migration и база.
 * Каждый тест получает свою чистую базу (контейнер на тест), чтобы начинать с пустой схемы.
 */
@Testcontainers
class MigrationsTest {

    @Container
    PostgreSQLContainer postgres = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE);

    @Test
    void allMigrationsApplyToEmptyDatabase() {
        Flyway flyway = flyway(null);

        flyway.migrate();

        MigrationInfoService info = flyway.info();
        assertThat(info.pending()).isEmpty();
        assertThat(info.applied()).hasSameSizeAs(info.all());
    }

    @Test
    void versionMigrationKeepsExistingDataAndSetsVersion0() throws SQLException {
        // 1. Схема как до миграции V2: в settings ещё нет колонки version
        flyway("1").migrate();
        execute("""
                INSERT INTO settings (account_id, floor_price, currency, blocked_domains)
                VALUES ('old-account', 1.5, 'USD', ARRAY['bad.com', 'spam.net'])
                """);

        // 2. Применяем все остальные миграции
        flyway(null).migrate();

        // 3. Старые данные на месте, version проставлена значением по умолчанию
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT * FROM settings WHERE account_id = 'old-account'")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getBigDecimal("floor_price")).isEqualByComparingTo("1.5");
            assertThat(row.getString("currency")).isEqualTo("USD");
            assertThat((String[]) row.getArray("blocked_domains").getArray()).containsExactly("bad.com", "spam.net");
            assertThat(row.getLong("version")).isEqualTo(0L);
        }
    }

    /** target = до какой версии миграций дойти; null — до последней. */
    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
