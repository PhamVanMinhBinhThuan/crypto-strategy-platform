package com.cryptostrategy.platform.marketdata.api.event;

@FunctionalInterface public interface CandleUpdateHandler { void onUpdate(CandleUpdate update); }
