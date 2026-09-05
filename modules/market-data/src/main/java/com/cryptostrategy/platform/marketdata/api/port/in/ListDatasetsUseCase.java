package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.util.List;

/** Lists immutable dataset snapshots available to authenticated research flows. */
public interface ListDatasetsUseCase {
    List<DatasetSnapshot> listRecentDatasets(java.util.UUID ownerUserId, int limit);

    default List<DatasetSnapshot> listRecentDatasets(int limit) {
        return listRecentDatasets(null, limit);
    }
}
