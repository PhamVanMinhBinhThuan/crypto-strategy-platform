package com.cryptostrategy.platform.worker.consumer;

import org.springframework.data.redis.connection.stream.MapRecord;

public interface MessageHandler {
    boolean canHandle(String streamKey, String messageType);
    void handle(MapRecord<String, String, String> record);
}
