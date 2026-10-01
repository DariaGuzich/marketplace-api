package com.example.marketplace.outbox;

public enum OutboxStatus {
    /** Ещё не доставлено в Serving. */
    NEW,
    /** Доставлено в Serving. */
    SENT,
    /** Конфиг невалидный, доставлять не будем. */
    FAILED
}
