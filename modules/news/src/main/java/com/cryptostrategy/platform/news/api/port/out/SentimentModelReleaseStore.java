package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;

public interface SentimentModelReleaseStore {
    void registerOrVerify(SentimentModelRelease release);
    default void activateForEnglish(String modelVersion) { }
}
