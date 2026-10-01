package com.example.marketplace.settings;

/** Превращается в 409 Conflict в ConflictExceptionHandler. */
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(String accountId, Long expected, Long actual) {
        super("Settings of account " + accountId + " have version " + actual + ", but client expected " + expected);
    }
}
