package com.cryptostrategy.platform.news.api;

import com.cryptostrategy.platform.news.api.model.CanonicalNewsUrl;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.LanguageCode;
import java.util.Objects;

/** Stable boundary for canonicalizing provider content before it enters the News domain. */
@FunctionalInterface
public interface NewsNormalizationPolicy {
    NormalizedNews normalize(String url, String titleHtml, String contentHtml, String language);

    record NormalizedNews(
            CanonicalNewsUrl url,
            String title,
            String content,
            LanguageCode language,
            ContentHash contentHash) {
        public NormalizedNews {
            Objects.requireNonNull(url, "url");
            title = required(title, "title");
            content = required(content, "content");
            Objects.requireNonNull(language, "language");
            Objects.requireNonNull(contentHash, "contentHash");
        }

        private static String required(String value, String field) {
            value = Objects.requireNonNull(value, field).trim();
            if (value.isEmpty()) throw new IllegalArgumentException(field + " is required");
            return value;
        }
    }
}
