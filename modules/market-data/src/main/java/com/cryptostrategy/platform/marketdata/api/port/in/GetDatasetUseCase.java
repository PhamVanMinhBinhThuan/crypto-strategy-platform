package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;

@FunctionalInterface public interface GetDatasetUseCase { DatasetSnapshot getDataset(DatasetVersionId datasetId); }
