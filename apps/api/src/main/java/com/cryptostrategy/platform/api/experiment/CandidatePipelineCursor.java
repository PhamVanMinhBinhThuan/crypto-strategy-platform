package com.cryptostrategy.platform.api.experiment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.cryptostrategy.platform.experiment.api.CandidateId;

record CandidatePipelineCursor(String view, CandidateId candidateId) {
    private static final String VERSION = "candidate-pipeline-v1";

    String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (VERSION + "\n" + view + "\n" + candidateId.value()).getBytes(StandardCharsets.UTF_8));
    }

    static CandidatePipelineCursor decode(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\n", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0]) || parts[1].isBlank()
                    || parts[2].isBlank()) throw new IllegalArgumentException("Invalid cursor");
            return new CandidatePipelineCursor(parts[1], new CandidateId(parts[2]));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Invalid candidate pipeline cursor", failure);
        }
    }
}
