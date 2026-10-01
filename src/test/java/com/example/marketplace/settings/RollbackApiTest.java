package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import com.example.marketplace.outbox.OutboxEntity;
import com.example.marketplace.outbox.OutboxRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Откат создаёт новую версию со значениями старой и пишет её в outbox, как любое изменение.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RollbackApiTest {

    @LocalServerPort
    int port;

    @Autowired
    OutboxRepository outboxRepository;

    @Test
    void rollbackCreatesNewVersionWithOldValues() {
        String accountId = "rollback-" + UUID.randomUUID();
        put(accountId, "1.5");   // версия 0
        put(accountId, "2.0");   // версия 1

        request().post("/accounts/{id}/settings/rollback?to_version=0", accountId)
                .then().statusCode(200)
                .body("floor_price", equalTo(1.5f))
                .body("version", equalTo(2));

        assertThat(outboxRepository.findByAccountIdAndVersion(accountId, 2L))
                .map(OutboxEntity::getPayload)
                .hasValueSatisfying(payload -> assertThat(payload.floorPrice()).isEqualByComparingTo("1.5"));
    }

    @Test
    void rollbackToUnknownVersionReturns404() {
        String accountId = "rollback-" + UUID.randomUUID();
        put(accountId, "1.5");

        request().post("/accounts/{id}/settings/rollback?to_version=42", accountId)
                .then().statusCode(404);
    }

    private void put(String accountId, String floorPrice) {
        request().body("""
                        {"floor_price": %s, "currency": "USD", "blocked_domains": []}
                        """.formatted(floorPrice))
                .put("/accounts/{id}/settings", accountId)
                .then().statusCode(200);
    }

    private RequestSpecification request() {
        return RestAssured.given().port(port).contentType(ContentType.JSON);
    }
}
