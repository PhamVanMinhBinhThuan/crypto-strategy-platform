package com.cryptostrategy.platform.search.api.port.out;

import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Search-owned durable state boundary; persistence implementations provide version fencing. */
public interface SearchRunStore {
    SearchRun create(SearchRun run);

    Optional<SearchRun> findById(SearchRunId searchRunId);

    Optional<SearchRun> findByExperimentId(String experimentId);

    Optional<SearchRun> findBySearchJobId(String searchJobId);

    /** Loads a proposal snapshot. A later save succeeds only while its version remains current. */
    Optional<SearchRunClaim> claim(SearchRunId searchRunId);

    /** Returns false for a stale claim; never overwrites a newer or terminal snapshot. */
    boolean save(SearchRunClaim claim, SearchRun replacement);

    /** Appends evidence under the same fence used by the associated state transition. */
    boolean appendDecision(SearchRunClaim claim, CoordinationDecision decision);

    /** Bounded scan ordered by updatedAt/searchRunId for reconstructing transient delivery work. */
    List<SearchRun> findRecoverable(Instant updatedBefore, int limit);
}
