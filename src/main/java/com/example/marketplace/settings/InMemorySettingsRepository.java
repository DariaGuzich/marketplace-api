package com.example.marketplace.settings;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySettingsRepository implements SettingsRepository {

    private final Map<String, Settings> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Settings> findByAccountId(String accountId) {
        return Optional.ofNullable(storage.get(accountId));
    }

    @Override
    public Settings save(String accountId, Settings settings) {
        storage.put(accountId, settings);
        return settings;
    }
}
