package com.cryptostrategy.platform.news.api.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record LanguageCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("(?:[a-z]{2,3}(?:-[a-z0-9]{2,8})*|und)");
    public static final LanguageCode ENGLISH = new LanguageCode("en");
    public LanguageCode {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Invalid language code");
    }
    @Override public String toString() { return value; }
}
