package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optimistic locking выключен (OPTIMISTIC_LOCKING_ENABLED=false): version клиента игнорируется,
 * и запись, сделанная на основе устаревших данных, молча затирает чужое изменение (lost update).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "marketplace.optimistic-locking.enabled=false")
@Import(TestcontainersConfiguration.class)
class LostUpdateApiTest {

    @LocalServerPort
    int port;

    @Test
    void staleWriteSilentlyOverwritesOtherChange() {
        String accountId = "lost-" + UUID.randomUUID();
        put(accountId, "1.5", "USD", null);              // версия 0; клиенты A и B прочитали её

        put(accountId, "1.5", "EUR", 0L);                // B меняет валюту → версия 1
        int status = put(accountId, "9.0", "USD", 0L);   // A меняет цену, но шлёт свою картину мира: валюта USD

        assertThat(status).isEqualTo(200);               // с включённой блокировкой здесь был бы 409
        JsonPath settings = RestAssured.given().port(port).get("/accounts/{id}/settings", accountId)
                .then().statusCode(200).extract().jsonPath();
        assertThat(settings.getString("currency")).isEqualTo("USD");   // изменение B потеряно
        assertThat(settings.getLong("version")).isEqualTo(2L);
    }

    private int put(String accountId, String floorPrice, String currency, Long version) {
        return RestAssured.given().port(port).contentType(ContentType.JSON)
                .body("""
                        {"floor_price": %s, "currency": "%s", "blocked_domains": [], "version": %s}
                        """.formatted(floorPrice, currency, version))
                .put("/accounts/{id}/settings", accountId)
                .statusCode();
    }
}
