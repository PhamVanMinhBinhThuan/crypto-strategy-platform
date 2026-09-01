# Contract & Domain Interfaces: Experiment, Manifest & Candidates

**Feature:** F-005 Experiment Persistence and Ownership  
**Status:** Canonical Design Contract  
**Date:** 2026-08-30  

This document defines the Java domain interfaces and records for the Experiment aggregate, immutable Manifest, Candidate definitions, and provenance snapshots.

---

## 1. Domain Types & State Machines

### 1.1. Experiment Status Enum
```java
package com.cryptostrategy.platform.experiment.api;

public enum ExperimentStatus {
    CREATED,        // Mutable preparation phase
    QUEUED,         // Manifest frozen, submitted for execution
    RUNNING,        // Active coordinator/workers running
    COMPLETED,      // All work finished successfully
    FAILED,         // Terminal failure
    STOP_REQUESTED, // User issued stop; halting work
    STOPPED         // Stopped safely
}
```

### 1.2. Provenance Value Objects (F-003 Resolved Integration & F-004 Integration Slot)
```java
package com.cryptostrategy.platform.experiment.api.provenance;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable Dataset provenance materialized from F-003's published Dataset Version contract.
 * F-003 defines no separate Dataset root; DatasetVersionId is the stable downstream identity.
 */
public record DatasetProvenanceSnapshot(
    DatasetVersionId datasetVersionId,
    String version,                 // F-003 Dataset/checksum contract ID; "candle-v1"
    String checksum,                // sha256:<64 lowercase hex>
    MarketProvider provider,
    TradingPairId tradingPairId,
    String canonicalPair,           // canonical BASE/QUOTE value from F-003
    Timeframe timeframe,
    String normalizationVersion,
    Instant rangeStart,
    Instant rangeEnd,
    int candleCount
) {}

/** Typed snapshot of single strategy component for composite configurations. */
public record StrategyComponentSnapshot(
    StrategyReference strategyReference,
    StrategyParameterSet parameters
) {}

/** Typed snapshot of Strategy / User Strategy provenance from F-004. */
public record StrategyProvenanceSnapshot(
    StrategyKind kind,
    Optional<StrategyReference> singleStrategy,
    StrategyParameterSet parameters,
    Optional<CombinationPolicyId> compositePolicyId,
    Optional<SemanticVersion> compositePolicyVersion,
    List<StrategyComponentSnapshot> components,
    Optional<UserStrategyVersionId> sourceUserStrategyVersionId,
    String strategyFingerprint
) {}
```

**F-003 integration rule**:

- `DatasetVersionId` is the only Dataset identity consumed by F-005; do not introduce a separate `DatasetId`.
- The snapshot is populated from F-003's immutable Dataset Version metadata and participates in the Experiment fingerprint.
- Dataset membership, provider normalization, checksum computation, and Dataset integrity verification remain owned by F-003.
- F-005 persists the `dataset_version_id` provenance anchor and must not mutate `market.*` Dataset evidence.
- Persistence stores the complete Dataset and Strategy provenance JSON snapshots; row mapping must never invent provider, pair, checksum, policy, version, or fingerprint defaults.

---

## 2. Experiment Aggregate Root & Manifest

### 2.1. Experiment Aggregate Interface
```java
package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Experiment {
    ExperimentId getId();
    UUID getOwnerUserId();
    Optional<ExperimentId> getDerivedFromExperimentId();
    Optional<ExperimentId> getReproducesExperimentId();
    String getName();
    ExperimentStatus getStatus();
    ExperimentManifest getManifest();
    Optional<Instant> getStartedAt();
    Optional<Instant> getCompletedAt();
    Optional<String> getFailureCode();
    Optional<String> getFailureMessage();
    Instant getCreatedAt();

    // Lifecycle Operations
    void updateCreatedConfiguration(String name, ExperimentManifest manifest);
    void freezeAndQueue(String fingerprint, Instant queuedAt);
    void markRunning(Instant startedAt);
    void markCompleted(Instant completedAt);
    void markFailed(String failureCode, String failureMessage, Instant failedAt);
    void requestStop(Instant stopRequestedAt);
    void confirmStopped(Instant stoppedAt);
}
```

### 2.2. Experiment Manifest Record
```java
package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record ExperimentManifest(
    String manifestVersion,
    DatasetProvenanceSnapshot dataset,
    StrategyProvenanceSnapshot strategy,
    Map<String, Object> backtestConfig,
    Map<String, Object> searchConfig,
    Map<String, Object> evaluationConfig,
    Optional<Map<String, Object>> sentimentConfig,
    String softwareVersion,
    String gitCommit,
    Optional<String> fingerprint,
    Instant createdAt
) {
    /** Computes deterministic canonical SHA-256 fingerprint over the canonical manifest payload.
     *  The stored fingerprint field itself is excluded from the hash input.
     */
    public static String computeFingerprint(ExperimentManifest manifest) {
        // Canonical SHA-256 computation logic over all fingerprinted manifest fields,
        // excluding the fingerprint field itself.
        return ...;
    }
}
```


### 2.3. Fingerprint Lifecycle

- While the parent Experiment is `CREATED`, `fingerprint` is empty (`Optional.empty()`).
- The `CREATED → QUEUED` transaction validates the Manifest, computes the canonical SHA-256 fingerprint, stores it, and freezes the Manifest atomically.
- Once the Experiment is `QUEUED` or later, the fingerprint must be present and the Manifest is immutable.
- `reproducesExperimentId` is distinct from `derivedFromExperimentId`: the former marks an explicit reproduction run; the latter marks a variation/lineage relationship.

---

## 3. Candidate Definition Record

```java
package com.cryptostrategy.platform.experiment.api;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record CandidateDefinition(
    CandidateId candidateId,
    ExperimentId experimentId,
    int generationIndex,
    Map<String, Object> definition,
    Optional<Map<String, Object>> generatorState,
    String fingerprint,
    Instant createdAt
) {}
```
