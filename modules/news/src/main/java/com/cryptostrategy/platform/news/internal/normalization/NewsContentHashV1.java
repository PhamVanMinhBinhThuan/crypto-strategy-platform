package com.cryptostrategy.platform.news.internal.normalization;

import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.LanguageCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class NewsContentHashV1 {
    public ContentHash hash(LanguageCode language, String title, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("news-canonical-v1\n".getBytes(StandardCharsets.US_ASCII));
            updateField(digest, language.value()); updateField(digest, title); updateField(digest, content);
            return new ContentHash("sha256:" + HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void updateField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':'); digest.update(bytes); digest.update((byte) '\n');
    }
}
