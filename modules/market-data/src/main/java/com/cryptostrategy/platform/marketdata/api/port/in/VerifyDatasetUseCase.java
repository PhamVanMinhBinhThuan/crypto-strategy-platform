package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetIntegrityResult;

@FunctionalInterface public interface VerifyDatasetUseCase { DatasetIntegrityResult verifyDataset(DatasetVersionId datasetId); }
