package com.cryptostrategy.platform.backtesting.internal.fingerprint;

import com.cryptostrategy.platform.backtesting.api.model.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** Canonical semantic fingerprint; runtime IDs and timestamps are deliberately excluded. */
public final class BacktestFingerprintV1 {
    public String calculate(BacktestRunCommand command, java.util.List<Trade> trades,
            Money finalCapital, EquityCurveSummary equity) {
        StringBuilder value = new StringBuilder("backtest-v1\n")
                .append(command.provenance()).append('\n').append(command.assumptions()).append('\n');
        for (Trade trade : trades) {
            value.append(trade.sequence()).append('|').append(trade.entryTime()).append('|')
                    .append(trade.exitTime()).append('|').append(trade.entryPrice().value().toPlainString()).append('|')
                    .append(trade.exitPrice().value().toPlainString()).append('|')
                    .append(trade.quantity().value().toPlainString()).append('|')
                    .append(trade.entryFee().value().toPlainString()).append('|')
                    .append(trade.exitFee().value().toPlainString()).append('|')
                    .append(trade.realizedPnl().toPlainString()).append('\n');
        }
        value.append(finalCapital.value().toPlainString()).append('|').append(equity.pointCount()).append('|')
                .append(equity.peakEquity().value().toPlainString()).append('|')
                .append(equity.troughEquity().value().toPlainString()).append('|')
                .append(equity.peakSequence()).append('|').append(equity.troughSequence()).append('|')
                .append(equity.curveDigest());
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
