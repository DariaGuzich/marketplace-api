package com.example.marketplace.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Оба уровня optimistic locking превращаются в 409 Conflict. В лог пишется, какой из них сработал.
 */
@RestControllerAdvice
public class ConflictExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ConflictExceptionHandler.class);

    /** Клиент прислал version, которая уже устарела (проверка в SettingsService). */
    @ExceptionHandler(VersionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void onStaleClientVersion(VersionConflictException e) {
        log.warn("409 by client version check: {}", e.getMessage());
    }

    /**
     * Два запроса прочитали одну и ту же версию и оба пытаются записать. Hibernate (@Version) выполняет
     * UPDATE ... WHERE version = <прочитанная>, у второго запроса обновляется 0 строк, и Hibernate бросает
     * это исключение. Без обработчика клиент получил бы 500.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void onConcurrentUpdate(ObjectOptimisticLockingFailureException e) {
        log.warn("409 by Hibernate @Version (concurrent update): {}", e.getMessage());
    }
}
