package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.BacktestConfigurationParser;
import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;
import com.cryptostrategy.platform.backtesting.api.port.in.RunBacktestUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultStore;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.backtesting.api.port.out.ResolvedStrategy;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.VerifyDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import java.util.Objects;

public final class RunBacktestService implements RunBacktestUseCase {
    private final GetFrozenBacktestExecutionUseCase frozenExecutions;
    private final GetDatasetUseCase datasets;
    private final VerifyDatasetUseCase datasetVerifier;
    private final DatasetCandleReader candleReader;
    private final FrozenStrategyResolver strategies;
    private final BacktestResultStore results;
    private final BacktestConfigurationParser configurationParser = new BacktestConfigurationParser();
    private final DeterministicBacktestEngine engine = new DeterministicBacktestEngine();

    public RunBacktestService(GetFrozenBacktestExecutionUseCase frozenExecutions, GetDatasetUseCase datasets,
            VerifyDatasetUseCase datasetVerifier, DatasetCandleReader candleReader,
            FrozenStrategyResolver strategies, BacktestResultStore results) {
        this.frozenExecutions = Objects.requireNonNull(frozenExecutions);
        this.datasets = Objects.requireNonNull(datasets);
        this.datasetVerifier = Objects.requireNonNull(datasetVerifier);
        this.candleReader = Objects.requireNonNull(candleReader);
        this.strategies = Objects.requireNonNull(strategies);
        this.results = Objects.requireNonNull(results);
    }

    @Override public BacktestResult run(BacktestRunCommand command) {
        Objects.requireNonNull(command);
        var frozen = frozenExecutions.getFrozenExecution(command.ownerUserId(), command.experimentId(),
                command.candidateId(), command.jobId(), command.attemptId());
        var manifest = frozen.manifest();
        var expected = manifest.datasetProvenance();
        var dataset = datasets.getDataset(expected.datasetVersionId());
        if (!dataset.version().equals(expected.version()) || !dataset.checksum().equals(expected.checksum())
                || dataset.candleCount() != expected.candleCount()) {
            throw new BacktestException(BacktestErrorCode.CHECKSUM_MISMATCH,
                    "Persisted Dataset does not match frozen Manifest provenance");
        }
        var integrity = datasetVerifier.verifyDataset(dataset.datasetVersionId());
        if (!integrity.valid()) throw new BacktestException(BacktestErrorCode.CHECKSUM_MISMATCH,
                integrity.detail().orElse("Dataset integrity failed"));
        var resolvedStrategy = strategies.resolve(manifest.strategyProvenance(), frozen.candidate());
        var resolved = new ResolvedStrategy(resolvedStrategy.strategy(), resolvedStrategy.requiredLookback(), resolvedStrategy.verifiedFingerprint());
        
        var backtestResolved = new ResolvedBacktestRun(command.experimentId(), command.candidateId(), command.jobId(),
                command.attemptId(), dataset,
                new BacktestProvenance(manifest.fingerprint(), dataset.checksum(), resolved.verifiedFingerprint()),
                configurationParser.parse(manifest.backtestConfig()), command.batchSize(),
                resolved.requiredLookback());
        return results.save(engine.run(backtestResolved, candleReader, resolved.strategy()));
    }
}
