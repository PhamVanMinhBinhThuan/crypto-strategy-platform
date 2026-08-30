package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetIntegrityResult;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.internal.checksum.CandleV1Checksum;
import com.cryptostrategy.platform.domain.api.market.Candle;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public final class DatasetIntegrityVerifier {
    private final DatasetCandleReader reader;
    private final CandleV1Checksum checksum;
    public DatasetIntegrityVerifier(DatasetCandleReader reader, CandleV1Checksum checksum) { this.reader = reader; this.checksum = checksum; }
    public DatasetIntegrityResult verify(DatasetSnapshot snapshot) {
        if (!CandleV1Checksum.VERSION.equals(snapshot.version())) return DatasetIntegrityResult.invalid("Unsupported Dataset version");
        CandleV1Checksum.Accumulator accumulator = checksum.accumulator();
        Set<com.cryptostrategy.platform.domain.api.market.CandleId> candleIds = new HashSet<>();
        Instant expectedOpenTime = snapshot.rangeStart();
        int sequence = 0;
        while (sequence < snapshot.candleCount()) {
            CandleBatch batch = reader.readCandles(snapshot.datasetVersionId(), sequence, Math.min(CandleBatch.MAX_BATCH_SIZE, snapshot.candleCount() - sequence));
            if (batch.members().isEmpty()) return DatasetIntegrityResult.invalid("Membership ended early");
            for (var member : batch.members()) {
                Candle candle = member.candle().candle();
                if (member.sequenceNo() != sequence) return DatasetIntegrityResult.invalid("Non-contiguous Dataset sequence");
                if (!candleIds.add(member.candle().candleId())) return DatasetIntegrityResult.invalid("Duplicate Dataset Candle");
                if (!candle.key().openTime().equals(expectedOpenTime)) return DatasetIntegrityResult.invalid("Dataset Candle continuity mismatch");
                if (!candle.key().provider().equals(snapshot.provider())
                        || !candle.key().tradingPair().tradingPairId().equals(snapshot.tradingPair().tradingPairId())
                        || candle.key().timeframe() != snapshot.timeframe()) {
                    return DatasetIntegrityResult.invalid("Dataset Candle is outside declared provenance");
                }
                accumulator.add(candle);
                expectedOpenTime = snapshot.timeframe().next(expectedOpenTime);
                sequence++;
            }
            sequence = batch.nextSequence();
        }
        if (sequence != snapshot.candleCount() || !expectedOpenTime.equals(snapshot.rangeEnd())
                || !accumulator.finish().equals(snapshot.checksum())) return DatasetIntegrityResult.invalid("Dataset count/checksum mismatch");
        return DatasetIntegrityResult.validResult();
    }
}
