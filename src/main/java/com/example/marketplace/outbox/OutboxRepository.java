package com.example.marketplace.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    // Spring Data строит SQL по имени метода: WHERE account_id = ? AND version = ?
    Optional<OutboxEntity> findByAccountIdAndVersion(String accountId, Long version);
}
