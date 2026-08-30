package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateDefinitionTest {

    private final ExperimentId experimentId = ExperimentId.generate();

    @Test
    @DisplayName("CandidateDefinition is immutable and enforces positive generation index and non-blank fingerprint")
    void candidateValidation() {
        CandidateId candidateId = CandidateId.generate();
        Map<String, Object> def = Map.of("fastPeriod", 12, "slowPeriod", 26);

        CandidateDefinition candidate = new CandidateDefinition(
                candidateId,
                experimentId,
                0,
                def,
                null,
                "sha256:candfp12345",
                Instant.now()
        );

        assertThat(candidate.candidateId()).isEqualTo(candidateId);
        assertThat(candidate.generationIndex()).isEqualTo(0);
        assertThat(candidate.definition()).isEqualTo(def);
        assertThat(candidate.fingerprint()).isEqualTo("sha256:candfp12345");

        assertThatThrownBy(() -> new CandidateDefinition(candidateId, experimentId, -1, def, null, "fp", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CandidateDefinition(candidateId, experimentId, 0, def, null, "", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
