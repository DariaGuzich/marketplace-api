package com.example.marketplace.settings;

import com.example.marketplace.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optimistic locking включён (по умолчанию): PUT с устаревшей version получает 409.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OptimisticLockingApiTest {

    @LocalServerPort
    int port;

    @Test
    void putWithStaleVersionReturns409() {
        String accountId = "lock-" + UUID.randomUUID();
        assertThat(put(accountId, "1.5", null)).isEqualTo(200);   // создана версия 0
        assertThat(put(accountId, "2.0", 0L)).isEqualTo(200);     // клиент видел 0 → версия 1

        int status = put(accountId, "3.0", 0L);                   // клиент всё ещё думает, что версия 0

        assertThat(status).isEqualTo(409);
    }

    @Test
    void twoParallelPutsWithSameVersion_oneSucceedsOneGets409() {
        String accountId = "race-" + UUID.randomUUID();
        put(accountId, "1.5", null);                               // версия 0

        // Оба запроса стартуют одновременно и оба видели версию 0
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> {
            await(start);
            return put(accountId, "2.0", 0L);
        });
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> {
            await(start);
            return put(accountId, "3.0", 0L);
        });
        start.countDown();

        // Кто из них победит, зависит от случая. 409 второй получит либо от проверки version
        // в SettingsService (если первый уже закоммитил), либо от @Version в Hibernate
        // (если оба прочитали версию 0 до коммита первого). Результат одинаковый.
        assertThat(List.of(first.join(), second.join())).containsExactlyInAnyOrder(200, 409);
        assertThat(version(accountId)).isEqualTo(1L);
    }

    private int put(String accountId, String floorPrice, Long version) {
        return RestAssured.given().port(port).contentType(ContentType.JSON)
                .body("""
                        {"floor_price": %s, "currency": "USD", "blocked_domains": [], "version": %s}
                        """.formatted(floorPrice, version))
                .put("/accounts/{id}/settings", accountId)
                .statusCode();
    }

    private long version(String accountId) {
        return RestAssured.given().port(port).get("/accounts/{id}/settings", accountId)
                .then().statusCode(200).extract().jsonPath().getLong("version");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
