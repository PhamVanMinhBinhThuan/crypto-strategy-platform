package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class CanonicalFingerprintCalculator {

    private final ObjectMapper objectMapper;

    public CanonicalFingerprintCalculator() {
        this.objectMapper = JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
    }

    public String calculate(ExperimentManifest manifest) {
        Map<String, Object> canonicalPayload = new LinkedHashMap<>();
        canonicalPayload.put("manifestVersion", manifest.manifestVersion());
        canonicalPayload.put("datasetProvenance", manifest.datasetProvenance());
        canonicalPayload.put("strategyProvenance", manifest.strategyProvenance());
        canonicalPayload.put("backtestConfig", manifest.backtestConfig());
        canonicalPayload.put("searchConfig", manifest.searchConfig());
        canonicalPayload.put("evaluationConfig", manifest.evaluationConfig());
        if (manifest.sentimentConfig() != null) {
            canonicalPayload.put("sentimentConfig", manifest.sentimentConfig());
        }
        canonicalPayload.put("softwareVersion", manifest.softwareVersion());
        canonicalPayload.put("gitCommit", manifest.gitCommit());

        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(canonicalPayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jsonBytes);
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to calculate canonical manifest fingerprint", e);
        }
    }
}
