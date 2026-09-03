package com.cryptostrategy.platform.execution.api.port.in;

public interface SearchCandidateAllocationUseCase {
    SearchCoordinationResult fillAvailableSlots(SearchCoordinationCommand command);
}
