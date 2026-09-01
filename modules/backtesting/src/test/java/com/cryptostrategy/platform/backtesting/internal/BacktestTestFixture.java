package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.job.*;
import com.cryptostrategy.platform.marketdata.api.model.*;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

final class BacktestTestFixture {
    final Instant start=Instant.parse("2026-01-01T00:00:00Z");
    final DatasetVersionId datasetId=new DatasetVersionId("00000000000000000000000001");
    final TradingPair pair;final DatasetSnapshot dataset;final List<DatasetMembership> members;
    final StrategyReference reference=new StrategyReference(new StrategyVersionId("00000000000000000000000009"),new StrategyPluginId("test"),new SemanticVersion(1,0,0));
    BacktestTestFixture(){Asset btc=new Asset(new AssetId("00000000000000000000000002"),new AssetSymbol("BTC"),Optional.empty(),true);Asset usdt=new Asset(new AssetId("00000000000000000000000003"),new AssetSymbol("USDT"),Optional.empty(),true);pair=new TradingPair(new TradingPairId("00000000000000000000000004"),btc,usdt,true);List<DatasetMembership> list=new ArrayList<>();for(int i=0;i<3;i++){Instant open=start.plusSeconds(i*60L);Candle candle=candle(open,BigDecimal.valueOf(100+i*10));list.add(new DatasetMembership(datasetId,i,new PersistedCandle(new CandleId(String.format("000000000000000000000000%02d",5+i)),candle)));}members=List.copyOf(list);dataset=new DatasetSnapshot(datasetId,"1",MarketProvider.BINANCE,pair,Timeframe.ONE_MINUTE,"v1",start,start.plusSeconds(180),3,"sha256:"+"0".repeat(64),start);}
    Candle candle(Instant open,BigDecimal price){return new Candle(new CandleKey(MarketProvider.BINANCE,pair,Timeframe.ONE_MINUTE,open),open.plusSeconds(60),price,price.add(BigDecimal.TEN),price.subtract(BigDecimal.TEN),price.add(BigDecimal.ONE),BigDecimal.ONE,true);}
    BacktestRunCommand command(int batch){return new BacktestRunCommand(new ExperimentId("0000000000000000000000000A"),new CandidateId("0000000000000000000000000B"),new JobId("0000000000000000000000000C"),new AttemptId("0000000000000000000000000D"),dataset,new BacktestProvenance("manifest","dataset","strategy"),BacktestAssumptions.mvp(BigDecimal.valueOf(1000),new BigDecimal("0.001"),new BigDecimal("0.001")),batch,3);}
    DatasetCandleReader reader(){return(id,from,size)->{int end=Math.min(from+size,members.size());return new CandleBatch(id,from,members.subList(from,end),end,end<members.size());};}
}
