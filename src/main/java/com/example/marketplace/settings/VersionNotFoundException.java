package com.example.marketplace.settings;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException(String accountId, Long version) {
        super("Account " + accountId + " has no settings version " + version);
    }
}
