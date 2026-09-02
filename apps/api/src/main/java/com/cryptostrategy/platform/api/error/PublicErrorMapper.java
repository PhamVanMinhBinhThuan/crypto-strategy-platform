package com.cryptostrategy.platform.api.error;

import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.experiment.api.error.ExperimentValidationException;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.error.NewsException;
import com.cryptostrategy.platform.api.transport.InvalidCursorException;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Component
public final class PublicErrorMapper {
    public MappedError map(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException) {
            return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "The request body is malformed.");
        }
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY_PARAMETER", "A query parameter is invalid.");
        }
        if (exception instanceof InvalidCursorException) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "The pagination cursor is invalid.");
        }
        if (exception instanceof MethodArgumentNotValidException
                || exception instanceof BindException
                || exception instanceof MissingRequestHeaderException
                || exception instanceof MissingServletRequestParameterException
                || exception instanceof IllegalArgumentException) {
            return error(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", "Request validation failed.");
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "The request media type is not supported.");
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                    "The request method is not supported.");
        }
        if (exception instanceof AuthenticationException) {
            return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }
        if (exception instanceof AccessDeniedException) {
            return error(HttpStatus.FORBIDDEN, "FORBIDDEN_ORIGIN", "The request is not permitted.");
        }
        if (exception instanceof NoResourceFoundException
                || exception instanceof ResourceInaccessibleException
                || exception instanceof com.cryptostrategy.platform.experiment.api.error
                        .ResourceInaccessibleException) {
            return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                    "The requested resource was not found.");
        }
        if (exception instanceof IdempotencyConflictException) {
            return error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                    "The idempotency key conflicts with an earlier request.");
        }
        if (exception instanceof InvalidStateTransitionException) {
            return error(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                    "The requested state transition is not allowed.");
        }
        if (exception instanceof ExperimentValidationException) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_RULE_VIOLATION",
                    "The request violates a domain rule.");
        }
        if (exception instanceof MarketDataException marketDataException) {
            return mapMarketData(marketDataException.code());
        }
        if (exception instanceof StrategyException strategyException) {
            return mapStrategy(strategyException.code());
        }
        if (exception instanceof NewsException newsException) {
            return mapNews(newsException.code());
        }
        if (exception instanceof BacktestException backtestException) {
            return mapBacktest(backtestException.code());
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private static MappedError mapMarketData(MarketDataErrorCode code) {
        return switch (code) {
            case INVALID_MARKET_QUERY -> error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MARKET_QUERY",
                    "The market data query is invalid.");
            case MARKET_PROVIDER_UNAVAILABLE -> retryable(HttpStatus.SERVICE_UNAVAILABLE,
                    "MARKET_PROVIDER_UNAVAILABLE", "The market data provider is unavailable.");
            case MARKET_PROVIDER_RATE_LIMITED -> retryable(HttpStatus.SERVICE_UNAVAILABLE,
                    "MARKET_PROVIDER_RATE_LIMITED", "The market data provider is rate limited.");
            case MARKET_DATA_GAP -> retryable(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_GAP",
                    "The requested market data is temporarily incomplete.");
            case MARKET_DATA_MAPPING_FAILED -> retryable(HttpStatus.BAD_GATEWAY, "MARKET_DATA_MAPPING_FAILED",
                    "The market data provider returned an invalid response.");
            case MARKET_DATA_INTEGRITY_CONFLICT, DATASET_INTEGRITY_FAILED -> error(HttpStatus.CONFLICT,
                    "DATASET_INTEGRITY_FAILED", "Dataset integrity verification failed.");
            case DATASET_NOT_FOUND -> error(HttpStatus.NOT_FOUND, "DATASET_NOT_FOUND",
                    "The requested dataset was not found.");
        };
    }

    private static MappedError mapStrategy(StrategyErrorCode code) {
        return switch (code) {
            case INVALID_PARAMETERS -> error(HttpStatus.UNPROCESSABLE_ENTITY, "STRATEGY_PARAMETERS_INVALID",
                    "Strategy parameters are invalid.");
            case INSUFFICIENT_DATA -> error(HttpStatus.UNPROCESSABLE_ENTITY, "STRATEGY_INPUT_INSUFFICIENT",
                    "The strategy input data is insufficient.");
            case STRATEGY_NOT_FOUND -> error(HttpStatus.NOT_FOUND, "STRATEGY_NOT_FOUND",
                    "The requested strategy was not found.");
            case UNSUPPORTED_VERSION -> error(HttpStatus.NOT_FOUND, "STRATEGY_VERSION_NOT_FOUND",
                    "The requested strategy version was not found.");
            case STRATEGY_CONFLICT, INTEGRITY_ERROR -> error(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                    "The strategy version conflicts with the current state.");
            case DUPLICATE_REGISTRATION -> error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE",
                    "The strategy is already registered.");
            case STORAGE_UNAVAILABLE -> retryable(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                    "The database is temporarily unavailable.");
        };
    }

    private static MappedError mapNews(NewsErrorCode code) {
        return switch (code) {
            case INVALID_INPUT -> error(HttpStatus.UNPROCESSABLE_ENTITY, "NEWS_CONTENT_INVALID",
                    "The news content is invalid.");
            case PROVIDER_FAILURE -> retryable(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                    "A required dependency is temporarily unavailable.");
            case INTEGRITY_CONFLICT, STALE_LEASE -> error(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                    "The news resource conflicts with the current state.");
            case PERSISTENCE_UNAVAILABLE -> retryable(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                    "The database is temporarily unavailable.");
            case INVALID_SENTIMENT_RESPONSE -> retryable(HttpStatus.BAD_GATEWAY, "SENTIMENT_RESPONSE_INVALID",
                    "The sentiment service returned an invalid response.");
        };
    }

    private static MappedError mapBacktest(BacktestErrorCode code) {
        return switch (code) {
            case INVALID_DATASET, INVALID_BATCH, INVALID_CANDLE, INVALID_STRATEGY, INVALID_ASSUMPTIONS,
                    INVALID_LINEAGE -> error(HttpStatus.UNPROCESSABLE_ENTITY, "BACKTEST_CONFIGURATION_INVALID",
                    "The backtest configuration is invalid.");
            case CHECKSUM_MISMATCH -> error(HttpStatus.CONFLICT, "DATASET_INTEGRITY_FAILED",
                    "Dataset integrity verification failed.");
            case ATTEMPT_NOT_SUCCEEDED, DUPLICATE_OUTCOME, PERSISTENCE_CONFLICT -> error(HttpStatus.CONFLICT,
                    "VERSION_CONFLICT", "The backtest result conflicts with the current state.");
        };
    }

    private static MappedError retryable(HttpStatus status, String code, String message) {
        return new MappedError(status, code, message, Map.of("retryable", true));
    }

    private static MappedError error(HttpStatus status, String code, String message) {
        return new MappedError(status, code, message, Map.of());
    }

    public record MappedError(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details) {
        public MappedError {
            details = SafeErrorDetails.copyOf(details);
        }
    }
}
