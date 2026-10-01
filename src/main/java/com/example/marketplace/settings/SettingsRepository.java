package com.example.marketplace.settings;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Слой хранения настроек. Реализацию (SQL-запросы к PostgreSQL) генерирует Spring Data JPA.
 */
public interface SettingsRepository extends JpaRepository<SettingsEntity, String> {
}
