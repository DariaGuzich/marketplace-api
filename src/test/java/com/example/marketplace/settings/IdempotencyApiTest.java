package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /blocked-domains неидемпотентен: каждый вызов добавляет домен.
 * Idempotency-Key делает повтор безопасным: второй запрос с тем же ключом домен не добавляет.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IdempotencyApiTest {

    @LocalServerPort
    int port;

    String accountId;

    @BeforeEach
    void createSettings() {
        accountId = "idem-" + UUID.randomUUID();
        request()
                .body("""
                        {"floor_price": 1.5, "currency": "USD", "blocked_domains": []}
                        """)
                .put("/accounts/{id}/settings", accountId)
                .then().statusCode(200);
    }

    @Test
    void sameKeyTwiceAddsDomainOnceAndReturnsSameResponse() {
        String key = UUID.randomUUID().toString();

        String first = addDomain("bad.com", key);
        String second = addDomain("bad.com", key);

        assertThat(second).isEqualTo(first);
        assertThat(blockedDomains()).containsExactly("bad.com");
    }

    @Test
    void withoutKeyDomainIsAddedTwice() {
        addDomain("bad.com", null);
        addDomain("bad.com", null);

        assertThat(blockedDomains()).containsExactly("bad.com", "bad.com");
    }

    private String addDomain(String domain, String idempotencyKey) {
        RequestSpecification request = request().body("{\"domain\": \"" + domain + "\"}");
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.post("/accounts/{id}/blocked-domains", accountId)
                .then().statusCode(200)
                .extract().asString();
    }

    private List<String> blockedDomains() {
        return request().get("/accounts/{id}/settings", accountId)
                .then().statusCode(200)
                .extract().jsonPath().getList("blocked_domains", String.class);
    }

    private RequestSpecification request() {
        return RestAssured.given().port(port).contentType(ContentType.JSON);
    }
}
