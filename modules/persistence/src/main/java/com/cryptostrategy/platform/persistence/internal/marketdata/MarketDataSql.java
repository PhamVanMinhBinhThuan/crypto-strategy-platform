package com.cryptostrategy.platform.persistence.internal.marketdata;

public final class MarketDataSql {
    public static final String INSERT_ASSET = "insert into market.asset(asset_id,symbol,name,active) values (?,?,?,?)";
    public static final String FIND_ASSET = "select asset_id,symbol,name,active from market.asset where symbol=?";
    public static final String INSERT_PAIR = "insert into market.trading_pair(trading_pair_id,base_asset_id,quote_asset_id,symbol,active) values (?,?,?,?,?)";
    public static final String FIND_PAIR_BY_ID = pairSelect() + " where tp.trading_pair_id=?";
    public static final String FIND_PAIR_BY_ASSETS = pairSelect() + " where tp.base_asset_id=? and tp.quote_asset_id=?";
    public static final String FIND_PAIR_BY_SYMBOLS = pairSelect() + " where ba.symbol=? and qa.symbol=?";
    public static final String INSERT_CANDLE = "insert into market.candle(candle_id,provider,trading_pair_id,timeframe,open_time,close_time,open,high,low,close,volume) values (?,?,?,?,?,?,?,?,?,?,?)";
    public static final String FIND_CANDLE_KEY = candleSelect() + " where c.provider=? and c.trading_pair_id=? and c.timeframe=? and c.open_time=?";
    public static final String FIND_CANDLE_RANGE = candleSelect() + " where c.provider=? and c.trading_pair_id=? and c.timeframe=? and c.open_time>=? and c.open_time<? order by c.open_time";
    public static final String INSERT_DATASET = "insert into market.dataset_version(dataset_version_id,version,provider,trading_pair_id,timeframe,normalization_version,range_start,range_end,candle_count,checksum,created_at) values (?,?,?,?,?,?,?,?,?,?,?)";
    public static final String FIND_DATASET_ID = datasetSelect() + " where dv.dataset_version_id=?";
    public static final String FIND_DATASET_CHECKSUM = datasetSelect() + " where dv.checksum=?";
    public static final String INSERT_MEMBER = "insert into market.dataset_candle(dataset_version_id,sequence_no,candle_id) values (?,?,?)";
    public static final String READ_MEMBERS = "select dc.sequence_no," + candleColumns() + " from market.dataset_candle dc join market.candle c on c.candle_id=dc.candle_id join market.trading_pair tp on tp.trading_pair_id=c.trading_pair_id join market.asset ba on ba.asset_id=tp.base_asset_id join market.asset qa on qa.asset_id=tp.quote_asset_id where dc.dataset_version_id=? and dc.sequence_no>=? order by dc.sequence_no limit ?";
    private MarketDataSql() { }
    private static String pairSelect() { return "select tp.trading_pair_id,tp.active tp_active,ba.asset_id ba_id,ba.symbol ba_symbol,ba.name ba_name,ba.active ba_active,qa.asset_id qa_id,qa.symbol qa_symbol,qa.name qa_name,qa.active qa_active from market.trading_pair tp join market.asset ba on ba.asset_id=tp.base_asset_id join market.asset qa on qa.asset_id=tp.quote_asset_id"; }
    private static String candleSelect() { return "select " + candleColumns() + " from market.candle c join market.trading_pair tp on tp.trading_pair_id=c.trading_pair_id join market.asset ba on ba.asset_id=tp.base_asset_id join market.asset qa on qa.asset_id=tp.quote_asset_id"; }
    private static String candleColumns() { return "c.candle_id,c.provider,c.trading_pair_id,c.timeframe,c.open_time,c.close_time,c.open,c.high,c.low,c.close,c.volume,tp.active tp_active,ba.asset_id ba_id,ba.symbol ba_symbol,ba.name ba_name,ba.active ba_active,qa.asset_id qa_id,qa.symbol qa_symbol,qa.name qa_name,qa.active qa_active"; }
    private static String datasetSelect() { return "select dv.dataset_version_id,dv.version,dv.provider,dv.timeframe,dv.normalization_version,dv.range_start,dv.range_end,dv.candle_count,dv.checksum,dv.created_at,tp.trading_pair_id,tp.active tp_active,ba.asset_id ba_id,ba.symbol ba_symbol,ba.name ba_name,ba.active ba_active,qa.asset_id qa_id,qa.symbol qa_symbol,qa.name qa_name,qa.active qa_active from market.dataset_version dv join market.trading_pair tp on tp.trading_pair_id=dv.trading_pair_id join market.asset ba on ba.asset_id=tp.base_asset_id join market.asset qa on qa.asset_id=tp.quote_asset_id"; }
}
