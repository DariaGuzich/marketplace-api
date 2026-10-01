package com.example.marketplace.settings;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlockedDomainsController {

    private final SettingsService service;

    public BlockedDomainsController(SettingsService service) {
        this.service = service;
    }

    @PostMapping(path = "/accounts/{account_id}/blocked-domains", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Домен добавлен, в ответе обновлённые настройки")
    @ApiResponse(responseCode = "404", description = "Настройки для аккаунта ещё не сохранены", content = @Content)
    @ApiResponse(responseCode = "409", description = "Настройки одновременно изменил другой запрос", content = @Content)
    public Settings addBlockedDomain(
            @PathVariable("account_id") String accountId,
            @Parameter(description = "Повтор запроса с тем же ключом вернёт ответ первого запроса и не добавит домен ещё раз")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AddBlockedDomainRequest request) {
        return service.addBlockedDomain(accountId, request.domain(), idempotencyKey);
    }
}
