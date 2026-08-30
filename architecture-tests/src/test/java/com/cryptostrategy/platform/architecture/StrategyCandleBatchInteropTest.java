package com.cryptostrategy.platform.architecture;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyCandleBatchInteropTest {
    @Test void batchMembershipFeedsOnlyBoundedCanonicalWindow(){DatasetVersionId dataset=new DatasetVersionId("01J00000000000000000000004");TradingPair pair=pair();List<DatasetMembership> members=new ArrayList<>();for(int i=0;i<30;i++){Candle candle=candle(pair,i);members.add(new DatasetMembership(dataset,i,new PersistedCandle(new CandleId(String.format("01J%023d",i+10)),candle)));}CandleBatch batch=new CandleBatch(dataset,0,members,30,false);List<Candle> rolling=new ArrayList<>();for(DatasetMembership member:batch.members()){rolling.add(member.candle().candle());if(rolling.size()>25)rolling.removeFirst();}StrategyContext context=new StrategyContext(pair,Timeframe.ONE_MINUTE,rolling,rolling.getLast().closeTime());assertEquals(25,context.candles().size());assertFalse(Arrays.stream(StrategyContext.class.getRecordComponents()).anyMatch(component->component.getType()==CandleBatch.class||component.getType()==DatasetSnapshot.class));}
    private static TradingPair pair(){return new TradingPair(new TradingPairId("01J00000000000000000000003"),new Asset(new AssetId("01J00000000000000000000001"),new AssetSymbol("BTC"),Optional.empty(),true),new Asset(new AssetId("01J00000000000000000000002"),new AssetSymbol("USDT"),Optional.empty(),true),true);}
    private static Candle candle(TradingPair pair,int minute){Instant open=Instant.parse("2026-01-01T00:00:00Z").plusSeconds(60L*minute);return new Candle(new CandleKey(MarketProvider.BINANCE,pair,Timeframe.ONE_MINUTE,open),open.plusSeconds(60),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,true);}
}
