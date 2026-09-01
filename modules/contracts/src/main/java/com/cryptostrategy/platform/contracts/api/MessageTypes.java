package com.cryptostrategy.platform.contracts.api;

public final class MessageTypes {
    private MessageTypes() {}

    public static final String BACKTEST_JOB = "BACKTEST_JOB";
    public static final String CANDIDATE_EVALUATED = "CANDIDATE_EVALUATED";
    public static final String DEAD_LETTER = "DEAD_LETTER";
    public static final String PROGRESS_EVENT = "PROGRESS_EVENT";
    public static final String LIFECYCLE_NOTIFICATION = "LIFECYCLE_NOTIFICATION";
    public static final String SEARCH_REQUEST = "SEARCH_REQUEST";

    public static final int CURRENT_VERSION = 1;
}
