package com.cryptostrategy.platform.worker.consumer;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class MessageDispatcher {

    private final List<MessageHandler> handlers;

    public MessageDispatcher(List<MessageHandler> handlers) {
        this.handlers = handlers != null ? List.copyOf(handlers) : List.of();
    }

    public void dispatch(MapRecord<String, String, String> record) {
        Objects.requireNonNull(record, "record cannot be null");
        String streamKey = record.getStream();
        String eventType = record.getValue().get("eventType");
        if (eventType == null) {
            eventType = record.getValue().get("header:eventType");
        }

        for (MessageHandler handler : handlers) {
            if (handler.canHandle(streamKey, eventType)) {
                handler.handle(record);
                return;
            }
        }
    }
}
