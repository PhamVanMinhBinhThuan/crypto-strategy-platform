package com.cryptostrategy.platform.search.api.model;

/** Durable reason that closed generation for a Search run. */
public enum SearchTerminalReason {
    MAXIMUM_CANDIDATES,
    SEARCH_SPACE_EXHAUSTED,
    MAXIMUM_DURATION,
    NO_IMPROVEMENT,
    EXPLICIT_STOP,
    TERMINAL_FAILURE
}
