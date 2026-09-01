package com.cryptostrategy.platform.news.api.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Opaque keyset cursor ordered by descending publication instant then News ID. */
public record NewsCursor(Instant publishedAt, NewsId newsId) {
    public NewsCursor {
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(newsId, "newsId");
    }
    public String encode() {
        String raw = publishedAt + "|" + newsId.value();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
    public static NewsCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(Objects.requireNonNull(encoded, "cursor")), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid News cursor");
            return new NewsCursor(Instant.parse(parts[0]), new NewsId(parts[1]));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Invalid News cursor", error);
        }
    }
}
