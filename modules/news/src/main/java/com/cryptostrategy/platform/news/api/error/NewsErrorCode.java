package com.cryptostrategy.platform.news.api.error;

/** Transport-neutral failure categories exposed by the News module. */
public enum NewsErrorCode {
    INVALID_INPUT,
    PROVIDER_FAILURE,
    INTEGRITY_CONFLICT,
    STALE_LEASE,
    PERSISTENCE_UNAVAILABLE,
    INVALID_SENTIMENT_RESPONSE
}
