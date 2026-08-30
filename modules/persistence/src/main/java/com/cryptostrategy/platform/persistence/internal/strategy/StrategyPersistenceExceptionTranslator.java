package com.cryptostrategy.platform.persistence.internal.strategy;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
public final class StrategyPersistenceExceptionTranslator {
    public StrategyException translate(DataAccessException exception){if(exception instanceof DataIntegrityViolationException)return new StrategyException(StrategyErrorCode.STRATEGY_CONFLICT,"Strategy state conflicts with current data",exception);return new StrategyException(StrategyErrorCode.STORAGE_UNAVAILABLE,"Strategy storage is unavailable",exception);}
}
