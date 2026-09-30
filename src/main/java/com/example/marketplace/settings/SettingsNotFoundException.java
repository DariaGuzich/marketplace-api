package com.example.marketplace.settings;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SettingsNotFoundException extends RuntimeException {

    public SettingsNotFoundException(String accountId) {
        super("Settings not found for account " + accountId);
    }
}
