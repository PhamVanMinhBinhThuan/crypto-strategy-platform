package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lists immutable dataset snapshots available to authenticated research flows. */
public interface ListDatasetsUseCase {
    List<DatasetSnapshot> listRecentDatasets(UUID ownerUserId, int limit);

    default List<DatasetSnapshot> listDatasetsPage(
            UUID ownerUserId, Instant beforeCreatedAt, String beforeDatasetId, int limit) {
        return listRecentDatasets(ownerUserId, Math.min(limit, 100));
    }

    default long countDatasets(UUID ownerUserId) {
        return listRecentDatasets(ownerUserId, 100).size();
    }

    default List<DatasetSnapshot> listRecentDatasets(int limit) {
        return listRecentDatasets(null, limit);
    }
}
