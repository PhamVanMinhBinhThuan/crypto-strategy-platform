package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.util.Optional;
import java.util.List;
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

    Optional<DatasetSnapshot> findByChecksum(String checksum);

    default List<DatasetSnapshot> listRecent(int limit) {
        return List.of();
    }

    default List<DatasetSnapshot> listRecent(UUID ownerUserId, int limit) {
        return listRecent(limit);
    }

    default void grantAccess(UUID ownerUserId, DatasetVersionId datasetId) {}
}
