package com.cryptostrategy.platform.worker.news.sentiment;

public final class SentimentClientException extends RuntimeException {
    private final boolean retryable; private final boolean countsTowardCircuit;
    public SentimentClientException(String message,boolean retryable,boolean countsTowardCircuit){super(message);this.retryable=retryable;this.countsTowardCircuit=countsTowardCircuit;}
    public SentimentClientException(String message,boolean retryable,boolean countsTowardCircuit,Throwable cause){super(message,cause);this.retryable=retryable;this.countsTowardCircuit=countsTowardCircuit;}
    public boolean retryable(){return retryable;} public boolean countsTowardCircuit(){return countsTowardCircuit;}
}
