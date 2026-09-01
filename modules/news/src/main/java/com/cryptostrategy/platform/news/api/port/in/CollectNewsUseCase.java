package com.cryptostrategy.platform.news.api.port.in;

import java.time.Instant;
import java.util.List;

public interface CollectNewsUseCase {
    List<CollectionOutcome> collectSince(Instant since);
    record CollectionOutcome(String provider, String url, Status status, String reason) {}
    enum Status { ACCEPTED, DUPLICATE, REJECTED, PROVIDER_FAILED }
}
