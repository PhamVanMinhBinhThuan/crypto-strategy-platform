package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.api.config.MarketDataProperties;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/datasets")
public final class DatasetController {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CreateDatasetUseCase createDataset;
    private final GetDatasetUseCase getDataset;
    private final MarketRequestMapper requests;
    private final MarketDataProperties properties;

    public DatasetController(
            @Qualifier("createDatasetUseCase") CreateDatasetUseCase createDataset,
            @Qualifier("getDatasetUseCase") GetDatasetUseCase getDataset,
            MarketRequestMapper requests,
            MarketDataProperties properties) {
        this.createDataset = createDataset;
        this.getDataset = getDataset;
        this.requests = requests;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<MarketDtos.DatasetResponse> create(
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestBody MarketDtos.CreateDatasetRequest request) {
        requireIdempotencyKey(idempotencyKey);
        var range = requests.range(
                request.pair(), request.timeframe(), request.startTime(), request.endTime());
        var query = requests.query(range, range.startTime(), range.endTime());
        var snapshot = createDataset.createDataset(new CreateDatasetCommand(
                query, properties.normalizationVersion(), properties.checksumContractVersion()));
        var response = MarketDtos.DatasetResponse.from(snapshot);
        return ResponseEntity.created(URI.create("/api/v1/datasets/" + response.datasetId()))
                .body(response);
    }

    @GetMapping("/{datasetId}")
    public MarketDtos.DatasetResponse get(@PathVariable String datasetId) {
        return MarketDtos.DatasetResponse.from(
                getDataset.getDataset(new DatasetVersionId(datasetId)));
    }

    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain between 1 and 255 characters");
        }
    }
}
