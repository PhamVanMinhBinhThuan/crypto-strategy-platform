package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.util.UUID;

@FunctionalInterface public interface GetDatasetUseCase {
    DatasetSnapshot getDataset(DatasetVersionId datasetId);

    default DatasetSnapshot getDataset(UUID ownerUserId, DatasetVersionId datasetId) {
        throw new MarketDataException(
                MarketDataErrorCode.DATASET_NOT_FOUND, "Dataset not found or inaccessible");
    }
}
