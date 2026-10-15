package com.aichatbot.exception;

import java.sql.SQLException;

/** Unchecked wrapper for SQLException so DAO callers are not forced to catch it everywhere. */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /** True when the underlying cause is a UNIQUE-key violation (e.g. duplicate email). */
    public boolean isDuplicateKey() {
        return getCause() instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_ENTRY;
    }
}
