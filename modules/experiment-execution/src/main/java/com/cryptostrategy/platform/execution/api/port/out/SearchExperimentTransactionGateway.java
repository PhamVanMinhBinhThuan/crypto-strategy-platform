package com.cryptostrategy.platform.execution.api.port.out;

/** Atomic boundary duy nhất cho graph thuộc cả Experiment và Search. */
public interface SearchExperimentTransactionGateway {
    StartSearchGraphResult start(StartSearchGraphCommand command);

    SearchAllocationResult allocate(AllocateSearchCandidateCommand command);
}
