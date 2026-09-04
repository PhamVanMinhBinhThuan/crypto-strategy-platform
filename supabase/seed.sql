-- Local development seed entrypoint.
-- Login-capable users are created through local Supabase Studio/Auth, not by inserting credentials here.

insert into market.asset (asset_id, symbol, name, active)
values
    ('01J00000000000000000000001', 'BTC', 'Bitcoin', true),
    ('01J00000000000000000000002', 'USDT', 'Tether USD', true)
on conflict (asset_id) do nothing;

insert into market.trading_pair (
    trading_pair_id,
    base_asset_id,
    quote_asset_id,
    symbol,
    active
)
values (
    '01J00000000000000000000003',
    '01J00000000000000000000001',
    '01J00000000000000000000002',
    'BTCUSDT',
    true
)
on conflict (trading_pair_id) do nothing;
