package com.example.marketplace.settings;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AccountsController {

    private final SettingsService service;

    public AccountsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping(path = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Account> listAccounts() {
        return service.listAccounts();
    }

    /**
     * Batch: настройки нескольких аккаунтов одним запросом, GET /settings?account_ids=a,b,c.
     * Аккаунты без настроек в ответ не попадают. Нужен BFF, чтобы избежать N+1 (DataLoader).
     */
    @GetMapping(path = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccountSettings> getSettingsBatch(@RequestParam("account_ids") List<String> accountIds) {
        return service.getMany(accountIds);
    }
}
