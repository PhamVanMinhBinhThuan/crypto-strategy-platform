package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.transport.InvalidCursorException;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

record CandidateCursor(int generationIndex, CandidateId candidateId) {
    private static final String VERSION = "candidate-v1";

    static CandidateCursor from(CandidateDefinition candidate) {
        return new CandidateCursor(candidate.generationIndex(), candidate.candidateId());
    }

    String encode() {
        String value = VERSION + "|" + generationIndex + "|" + candidateId.value();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static CandidateCursor decode(String value) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw invalid(null);
            }
            int generation = Integer.parseInt(parts[1]);
            if (generation < 0 || parts[2].isBlank()) {
                throw invalid(null);
            }
            return new CandidateCursor(generation, new CandidateId(parts[2]));
        } catch (InvalidCursorException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid(exception);
        }
    }

    private static InvalidCursorException invalid(Throwable cause) {
        return cause == null
                ? new InvalidCursorException("Candidate cursor is invalid")
                : new InvalidCursorException("Candidate cursor is invalid", cause);
    }
}
