package com.cryptostrategy.platform.marketdata.api.event;

@FunctionalInterface public interface ConnectionStateHandler { void onState(ConnectionState state); }
