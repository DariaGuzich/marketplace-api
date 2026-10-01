package com.example.marketplace.settings;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * API-тесты: приложение поднимается целиком на случайном порту, запросы идут по настоящему HTTP.
 * Проверяем контракт так, как его видит клиент (BFF): пути, статусы и JSON в snake_case.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SettingsApiTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void getReturns404WhenSettingsNotSaved() {
        RestAssured.given()
                .when().get("/accounts/unknown-account/settings")
                .then().statusCode(404);
    }

    @Test
    void putSavesSettingsAndGetReturnsThemInSnakeCase() {
        String body = """
                {"floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com", "spam.net"]}
                """;

        RestAssured.given()
                .contentType(ContentType.JSON).body(body)
                .when().put("/accounts/acc-put/settings")
                .then().statusCode(200);

        RestAssured.given()
                .when().get("/accounts/acc-put/settings")
                .then().statusCode(200)
                .body("floor_price", equalTo(1.5f))
                .body("currency", equalTo("USD"))
                .body("blocked_domains", contains("bad.com", "spam.net"));
    }

    @Test
    void putReturns400WhenRequiredFieldMissing() {
        String body = """
                {"floor_price": 1.5, "currency": "USD"}
                """;

        RestAssured.given()
                .contentType(ContentType.JSON).body(body)
                .when().put("/accounts/acc-invalid/settings")
                .then().statusCode(400);
    }
}
