package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetIntegrityResult;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.VerifyDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore;

public final class DatasetService implements CreateDatasetUseCase, GetDatasetUseCase, VerifyDatasetUseCase {
    private final LoadHistoricalCandlesUseCase historical;
    private final DatasetAssembler assembler;
    private final DatasetStore store;
    private final DatasetIntegrityVerifier verifier;
    public DatasetService(LoadHistoricalCandlesUseCase historical, DatasetAssembler assembler, DatasetStore store, DatasetIntegrityVerifier verifier) {
        this.historical = historical; this.assembler = assembler; this.store = store; this.verifier = verifier;
    }
    @Override public DatasetSnapshot createDataset(CreateDatasetCommand command) {
        DatasetFinalization finalization = assembler.assemble(command, historical.loadHistoricalCandles(command.query()).candles());
        return store.finalizeAtomically(finalization);
    }
    @Override public DatasetSnapshot getDataset(DatasetVersionId datasetId) {
        DatasetSnapshot snapshot = store.find(datasetId).orElseThrow(() -> new MarketDataException(MarketDataErrorCode.DATASET_NOT_FOUND, "Dataset not found"));
        if (!verifier.verify(snapshot).valid()) throw new MarketDataException(MarketDataErrorCode.DATASET_INTEGRITY_FAILED, "Dataset integrity failed");
        return snapshot;
    }
    @Override public DatasetIntegrityResult verifyDataset(DatasetVersionId datasetId) { return store.find(datasetId).map(verifier::verify).orElseThrow(() -> new MarketDataException(MarketDataErrorCode.DATASET_NOT_FOUND, "Dataset not found")); }
}
