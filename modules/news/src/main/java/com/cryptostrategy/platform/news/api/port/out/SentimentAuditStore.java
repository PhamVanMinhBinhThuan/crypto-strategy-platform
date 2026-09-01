package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.*;
import java.util.Optional;

public interface SentimentAuditStore { Optional<SentimentAuditRecord> findLatest(NewsId newsId); }
