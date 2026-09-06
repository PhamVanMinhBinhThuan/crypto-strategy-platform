package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.api.config.MarketDataProperties;
import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.HistoryCursor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.ListDatasetsUseCase;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ListDatasetsUseCase listDatasets;
    private final MarketRequestMapper requests;
    private final MarketDataProperties properties;
    private final IdempotencyCommandExecutor idempotency;
    private final PageRequestMapper pages;

    public DatasetController(
            @Qualifier("createDatasetUseCase") CreateDatasetUseCase createDataset,
            @Qualifier("getDatasetUseCase") GetDatasetUseCase getDataset,
            @Qualifier("listDatasetsUseCase") ListDatasetsUseCase listDatasets,
            MarketRequestMapper requests,
            MarketDataProperties properties,
            IdempotencyCommandExecutor idempotency) {
        this(createDataset, getDataset, listDatasets, requests, properties, idempotency,
                new PageRequestMapper());
    }

    @Autowired
    public DatasetController(
            @Qualifier("createDatasetUseCase") CreateDatasetUseCase createDataset,
            @Qualifier("getDatasetUseCase") GetDatasetUseCase getDataset,
            @Qualifier("listDatasetsUseCase") ListDatasetsUseCase listDatasets,
            MarketRequestMapper requests,
            MarketDataProperties properties,
            IdempotencyCommandExecutor idempotency,
            PageRequestMapper pages) {
        this.createDataset = createDataset;
        this.getDataset = getDataset;
        this.listDatasets = listDatasets;
        this.requests = requests;
        this.properties = properties;
        this.idempotency = idempotency;
        this.pages = pages;
    }

    @GetMapping
    public MarketDtos.DatasetListResponse list(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserContext user,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        var page = pages.map(limit, cursor, 50, 100);
        HistoryCursor after = page.cursor()
                .map(value -> HistoryCursor.decode(value, "datasets"))
                .orElse(null);
        var ordered = listDatasets.listDatasetsPage(
                user.userId(),
                after == null ? null : after.timestamp(),
                after == null ? null : after.id(),
                page.limit() + 1);
        boolean hasMore = ordered.size() > page.limit();
        var selected = hasMore ? ordered.subList(0, page.limit()) : ordered;
        String nextCursor = hasMore && !selected.isEmpty()
                ? new HistoryCursor("datasets", selected.getLast().createdAt(),
                        selected.getLast().datasetVersionId().value()).encode()
                : null;
        return new MarketDtos.DatasetListResponse(
                selected.stream().map(MarketDtos.DatasetResponse::from).toList(),
                nextCursor, hasMore, listDatasets.countDatasets(user.userId()));
    }

    public MarketDtos.DatasetListResponse list(AuthenticatedUserContext user, int limit) {
        return new MarketDtos.DatasetListResponse(
                listDatasets.listRecentDatasets(user.userId(), limit).stream()
                        .map(MarketDtos.DatasetResponse::from)
                        .toList());
    }

    @PostMapping
    public ResponseEntity<MarketDtos.DatasetResponse> create(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestBody MarketDtos.CreateDatasetRequest request) {
        requireIdempotencyKey(idempotencyKey);
        var response = idempotency.execute(user.userId(), "CREATE_DATASET", idempotencyKey,
                request, MarketDtos.DatasetResponse.class, (key, requestHash) -> {
                    var range = requests.range(
                            request.pair(), request.timeframe(), request.startTime(), request.endTime());
                    var query = requests.query(range, range.startTime(), range.endTime());
                    var snapshot = createDataset.createDataset(new CreateDatasetCommand(
                            query, properties.normalizationVersion(), properties.checksumContractVersion(),
                            user.userId()));
                    return MarketDtos.DatasetResponse.from(snapshot);
                });
        return ResponseEntity.created(URI.create("/api/v1/datasets/" + response.datasetId()))
                .body(response);
    }

    @GetMapping("/{datasetId}")
    public MarketDtos.DatasetResponse get(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String datasetId) {
        return MarketDtos.DatasetResponse.from(
                getDataset.getDataset(user.userId(), new DatasetVersionId(datasetId)));
    }

    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain between 1 and 255 characters");
        }
    }
}
