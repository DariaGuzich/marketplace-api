package com.example.marketplace;

import org.springframework.boot.SpringApplication;

/**
 * Запуск приложения с PostgreSQL из Testcontainers вместо внешней базы.
 * Используется в mvn verify для генерации openapi.json (см. pom.xml), чтобы для неё не нужен был docker-compose.
 */
public class TestMarketplaceApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(MarketplaceApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
