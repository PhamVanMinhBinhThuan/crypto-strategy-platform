package com.cryptostrategy.platform.news.api.model;

import java.net.URI;
import java.util.Objects;

public record CanonicalNewsUrl(URI value) {
    public CanonicalNewsUrl {
        Objects.requireNonNull(value, "value");
        var scheme = value.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || value.getHost() == null)
            throw new IllegalArgumentException("News URL must be an absolute HTTP(S) URL");
        if (value.getUserInfo() != null || value.getFragment() != null)
            throw new IllegalArgumentException("Canonical News URL cannot contain user info or fragment");
    }
    public CanonicalNewsUrl(String value) { this(URI.create(value)); }
    @Override public String toString() { return value.toASCIIString(); }
}
