package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.util.Optional;

public interface DatasetStore {
    DatasetSnapshot finalizeAtomically(DatasetFinalization finalization);

    Optional<DatasetSnapshot> find(DatasetVersionId datasetId);

    Optional<DatasetSnapshot> findByChecksum(String checksum);
}
