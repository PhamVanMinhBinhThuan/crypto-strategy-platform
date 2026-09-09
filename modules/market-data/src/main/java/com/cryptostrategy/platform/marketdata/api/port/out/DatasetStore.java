package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetStore {
    DatasetSnapshot finalizeAtomically(DatasetFinalization finalization);

    default DatasetSnapshot finalizeAtomically(
            DatasetFinalization finalization, UUID ownerUserId) {
        DatasetSnapshot snapshot = finalizeAtomically(finalization);
        if (ownerUserId != null) grantAccess(ownerUserId, snapshot.datasetVersionId());
        return snapshot;
    }

    Optional<DatasetSnapshot> find(DatasetVersionId datasetId);

    default Optional<DatasetSnapshot> findAccessible(
            UUID ownerUserId, DatasetVersionId datasetId) {
        return Optional.empty();
    }

    Optional<DatasetSnapshot> findByChecksum(String checksum);

    default List<DatasetSnapshot> listRecent(int limit) {
        return List.of();
    }

    default List<DatasetSnapshot> listRecent(UUID ownerUserId, int limit) {
        return ownerUserId == null ? listRecent(limit) : List.of();
    }

    default List<DatasetSnapshot> listPage(
            UUID ownerUserId, Instant beforeCreatedAt, String beforeDatasetId, int limit) {
        return listRecent(ownerUserId, Math.min(limit, 100));
    }

    default long count(UUID ownerUserId) {
        return listRecent(ownerUserId, 100).size();
    }

    default void grantAccess(UUID ownerUserId, DatasetVersionId datasetId) {}
}
