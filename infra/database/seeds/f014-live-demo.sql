begin;

-- F014 demo reference data only. This file is intentionally not a production
-- migration: it may be applied repeatedly to an approved demo environment.
insert into market.asset (asset_id, symbol, name, active)
values
    ('01F01400000000000000000001', 'BTC', 'Bitcoin', true),
    ('01F01400000000000000000002', 'USDT', 'Tether USD', true)
on conflict do nothing;

do $seed$
begin
    if not exists (
        select 1 from market.asset where symbol = 'BTC' and active
    ) or not exists (
        select 1 from market.asset where symbol = 'USDT' and active
    ) then
        raise exception 'F014 seed conflict: BTC and USDT must exist and be active';
    end if;
end
$seed$;

insert into market.trading_pair (
    trading_pair_id,
    base_asset_id,
    quote_asset_id,
    symbol,
    active
)
select
    '01F01400000000000000000003',
    base.asset_id,
    quote.asset_id,
    'BTCUSDT',
    true
from market.asset base
cross join market.asset quote
where base.symbol = 'BTC'
  and quote.symbol = 'USDT'
on conflict do nothing;

do $seed$
begin
    if not exists (
        select 1
        from market.trading_pair pair
        join market.asset base on base.asset_id = pair.base_asset_id
        join market.asset quote on quote.asset_id = pair.quote_asset_id
        where base.symbol = 'BTC'
          and quote.symbol = 'USDT'
          and pair.symbol = 'BTCUSDT'
          and pair.active
          and base.active
          and quote.active
    ) then
        raise exception 'F014 seed conflict: active BTC/USDT pair could not be established';
    end if;
end
$seed$;

commit;

select 'f014_market_reference=ready'
where exists (
    select 1
    from market.trading_pair pair
    join market.asset base on base.asset_id = pair.base_asset_id
    join market.asset quote on quote.asset_id = pair.quote_asset_id
    where base.symbol = 'BTC'
      and quote.symbol = 'USDT'
      and pair.symbol = 'BTCUSDT'
      and pair.active
      and base.active
      and quote.active
);
