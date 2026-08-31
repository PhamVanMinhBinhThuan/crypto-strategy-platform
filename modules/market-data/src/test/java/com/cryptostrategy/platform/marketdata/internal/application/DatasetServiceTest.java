package com.cryptostrategy.platform.marketdata.internal.application;
import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.*;import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.marketdata.api.model.*;import com.cryptostrategy.platform.marketdata.internal.checksum.CandleV1Checksum;import java.time.*;import java.util.*;import org.junit.jupiter.api.Test;
class DatasetServiceTest {
 @Test void oneHundredInputPermutationsProduceOneCanonicalChecksum(){DatasetAssembler assembler=new DatasetAssembler(Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"),ZoneOffset.UTC),new CandleV1Checksum());CreateDatasetCommand command=new CreateDatasetCommand(query(3),"binance-v1",CandleV1Checksum.VERSION);Set<String> checksums=new HashSet<>();for(int seed=0;seed<100;seed++){List<com.cryptostrategy.platform.domain.api.market.Candle> values=new ArrayList<>(List.of(candle(0,"1"),candle(1,"2"),candle(2,"3")));Collections.shuffle(values,new Random(seed));checksums.add(assembler.assemble(command,values).snapshot().checksum());}assertEquals(1,checksums.size());}
}
