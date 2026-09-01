package com.cryptostrategy.platform.backtesting.api.error;
public final class BacktestException extends RuntimeException {
    private static final long serialVersionUID=1L; private final BacktestErrorCode code;
    public BacktestException(BacktestErrorCode code,String message){super(message);this.code=code;}
    public BacktestErrorCode code(){return code;}
}
