package com.cryptostrategy.platform.strategy.api.model.user.query;
import java.util.Optional;
public record UsableStrategyPageRequest(int systemPageSize, Optional<String> systemCursor,
        int privatePageSize, Optional<String> privateCursor) {
    public static final int DEFAULT_SIZE=20; public static final int MAX_SIZE=100;
    public UsableStrategyPageRequest { systemCursor=systemCursor==null?Optional.empty():systemCursor; privateCursor=privateCursor==null?Optional.empty():privateCursor; validate(systemPageSize); validate(privatePageSize); }
    public static UsableStrategyPageRequest defaults(){return new UsableStrategyPageRequest(DEFAULT_SIZE,Optional.empty(),DEFAULT_SIZE,Optional.empty());}
    private static void validate(int size){if(size<1||size>MAX_SIZE) throw new IllegalArgumentException("Page size must be between 1 and 100");}
}
