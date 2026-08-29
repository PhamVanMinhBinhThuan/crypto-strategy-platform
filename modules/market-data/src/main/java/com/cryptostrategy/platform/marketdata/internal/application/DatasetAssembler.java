package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.internal.checksum.CandleV1Checksum;
import com.cryptostrategy.platform.marketdata.internal.validation.CandleSetValidator;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class DatasetAssembler {
    private final Clock clock;
    private final CandleV1Checksum checksum;
    public DatasetAssembler(Clock clock, CandleV1Checksum checksum) { this.clock = Objects.requireNonNull(clock); this.checksum = Objects.requireNonNull(checksum); }
    public DatasetFinalization assemble(CreateDatasetCommand command, List<Candle> candles) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(candles, "candles");
        if (!CandleV1Checksum.VERSION.equals(command.version())) {
            throw new IllegalArgumentException("Unsupported Dataset version: " + command.version());
        }
        if (candles.isEmpty()) throw new IllegalArgumentException("Dataset requires at least one Candle");
        List<Candle> canonical = CandleSetValidator.normalizeComplete(command.query(), candles);
        String digest = checksum.calculate(canonical);
        DatasetSnapshot snapshot = new DatasetSnapshot(DatasetVersionId.generate(), command.version(), command.query().provider(),
                command.query().tradingPair(), command.query().timeframe(), command.normalizationVersion(), command.query().startTime(),
                command.query().endTime(), canonical.size(), digest, clock.instant());
        return new DatasetFinalization(snapshot, canonical);
    }
}
