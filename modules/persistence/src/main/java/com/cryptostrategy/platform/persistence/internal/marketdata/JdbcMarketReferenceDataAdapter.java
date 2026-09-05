package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcMarketReferenceDataAdapter implements MarketReferenceDataStore {
    private final JdbcTemplate jdbc;
    public JdbcMarketReferenceDataAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Asset resolveAsset(Asset asset) {
        Optional<Asset> existing = findAsset(asset.symbol()); if (existing.isPresent()) return existing.get();
        jdbc.update(MarketDataSql.INSERT_ASSET, asset.assetId().value(), asset.symbol().value(), asset.name().orElse(null), asset.active());
        return findAsset(asset.symbol()).orElseThrow(() -> new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Asset conflict"));
    }
    @Override public TradingPair resolveTradingPair(TradingPair pair) {
        Asset base = resolveAsset(pair.baseAsset()); Asset quote = resolveAsset(pair.quoteAsset());
        Optional<TradingPair> existing = jdbc.query(MarketDataSql.FIND_PAIR_BY_ASSETS, (rs, row) -> MarketDataRows.pair(rs), base.assetId().value(), quote.assetId().value()).stream().findFirst();
        if (existing.isPresent()) return existing.get();
        jdbc.update(MarketDataSql.INSERT_PAIR, pair.tradingPairId().value(), base.assetId().value(), quote.assetId().value(), base.symbol().value() + quote.symbol().value(), pair.active());
        return jdbc.query(MarketDataSql.FIND_PAIR_BY_ASSETS, (rs, row) -> MarketDataRows.pair(rs), base.assetId().value(), quote.assetId().value()).stream().findFirst()
                .orElseThrow(() -> new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Trading Pair conflict"));
    }
    @Override public Optional<TradingPair> findTradingPair(TradingPairId tradingPairId) { return jdbc.query(MarketDataSql.FIND_PAIR_BY_ID, (rs, row) -> MarketDataRows.pair(rs), tradingPairId.value()).stream().findFirst(); }
    @Override public Optional<TradingPair> findTradingPair(AssetSymbol baseAsset, AssetSymbol quoteAsset) {
        return jdbc.query(MarketDataSql.FIND_PAIR_BY_SYMBOLS, (rs, row) -> MarketDataRows.pair(rs), baseAsset.value(), quoteAsset.value()).stream().findFirst();
    }
    @Override public Optional<Asset> findAsset(AssetSymbol symbol) {
        return jdbc.query(MarketDataSql.FIND_ASSET, (rs, row) -> new Asset(new AssetId(rs.getString("asset_id")), new AssetSymbol(rs.getString("symbol")), Optional.ofNullable(rs.getString("name")), rs.getBoolean("active")), symbol.value()).stream().findFirst();
    }
}
