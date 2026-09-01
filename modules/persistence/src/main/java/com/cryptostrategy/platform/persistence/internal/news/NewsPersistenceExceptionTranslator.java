package com.cryptostrategy.platform.persistence.internal.news;

import java.sql.SQLException;
import org.springframework.dao.DataAccessException;

public final class NewsPersistenceExceptionTranslator {
    public RuntimeException translate(DataAccessException error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql && ("40P01".equals(sql.getSQLState()) || "40001".equals(sql.getSQLState())))
                return new RecoverableNewsPersistenceException(sql.getSQLState(), error);
            current = current.getCause();
        }
        return error;
    }
    public static final class RecoverableNewsPersistenceException extends RuntimeException {
        private final String sqlState;
        public RecoverableNewsPersistenceException(String sqlState, Throwable cause) { super("Recoverable News transaction failure: " + sqlState, cause); this.sqlState = sqlState; }
        public String sqlState() { return sqlState; }
    }
}
