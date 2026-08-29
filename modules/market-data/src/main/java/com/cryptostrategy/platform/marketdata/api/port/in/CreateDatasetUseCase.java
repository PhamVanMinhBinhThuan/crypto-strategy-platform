package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;

@FunctionalInterface public interface CreateDatasetUseCase { DatasetSnapshot createDataset(CreateDatasetCommand command); }
