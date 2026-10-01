package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AccountsApiTest {

    @LocalServerPort
    int port;

    @Test
    void listAccountsAndBatchSettings() {
        String a = "batch-a-" + UUID.randomUUID();
        String b = "batch-b-" + UUID.randomUUID();
        put(a, "1.5");
        put(b, "2.5");

        List<String> accounts = request().get("/accounts")
                .then().statusCode(200).extract().jsonPath().getList("account_id", String.class);
        assertThat(accounts).contains(a, b);

        // Один запрос вместо трёх; аккаунт без настроек просто отсутствует в ответе
        List<String> found = request().get("/settings?account_ids={a},{b},missing", a, b)
                .then().statusCode(200).extract().jsonPath().getList("account_id", String.class);
        assertThat(found).containsExactlyInAnyOrder(a, b);
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
