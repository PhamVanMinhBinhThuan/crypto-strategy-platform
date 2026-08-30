package com.cryptostrategy.platform.strategies.internal.ma;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class MovingAverageCrossoverStrategyTest {
    @Test void deterministicAndInsufficientDataIsNotHold(){MovingAverageCrossoverPlugin plugin=new MovingAverageCrossoverPlugin();Strategy strategy=plugin.create(new com.cryptostrategy.platform.strategy.internal.parameter.StrategyParameterValidator().resolve(plugin.descriptor().parameterSchema(),Map.of()));StrategyContext context=context(25);StrategyDecision expected=strategy.evaluate(context);for(int i=0;i<100;i++)assertEquals(expected,strategy.evaluate(context));StrategyException error=assertThrows(StrategyException.class,()->strategy.evaluate(context(24)));assertEquals(StrategyErrorCode.INSUFFICIENT_DATA,error.code());}
    private static StrategyContext context(int count){Asset btc=new Asset(new AssetId("01J00000000000000000000001"),new AssetSymbol("BTC"),Optional.empty(),true);Asset usdt=new Asset(new AssetId("01J00000000000000000000002"),new AssetSymbol("USDT"),Optional.empty(),true);TradingPair pair=new TradingPair(new TradingPairId("01J00000000000000000000003"),btc,usdt,true);Instant start=Instant.parse("2026-01-01T00:00:00Z");List<Candle> candles=new ArrayList<>();for(int i=0;i<count;i++){Instant open=start.plusSeconds(60L*i);BigDecimal price=BigDecimal.valueOf(100L+i);candles.add(new Candle(new CandleKey(MarketProvider.BINANCE,pair,Timeframe.ONE_MINUTE,open),open.plusSeconds(60),price,price,price,price,BigDecimal.ONE,true));}return new StrategyContext(pair,Timeframe.ONE_MINUTE,candles,candles.getLast().closeTime());}
}
