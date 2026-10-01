package com.example.marketplace.settings;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * account_id пока приходит из пути. Когда появится авторизация, аутентификация встанет
 * фильтром Spring Security перед контроллером, а проверка доступа к аккаунту — в SettingsService.
 */
@RestController
@RequestMapping("/accounts/{account_id}/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Настройки аккаунта")
    @ApiResponse(responseCode = "404", description = "Настройки для аккаунта ещё не сохранены", content = @Content)
    public Settings getSettings(@PathVariable("account_id") String accountId) {
        return service.get(accountId);
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Settings updateSettings(@PathVariable("account_id") String accountId,
                                   @Valid @RequestBody SettingsValues values) {
        return service.update(accountId, values);
    }
}
