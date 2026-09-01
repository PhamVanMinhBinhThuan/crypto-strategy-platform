package com.cryptostrategy.platform.api.transport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Maps capability pages to the common public collection envelope. */
@Component
public final class PageResponseMapper {

    public <Item> PageResponse<Item> map(
            List<Item> items,
            Optional<String> nextCursor) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(nextCursor, "nextCursor");
        String publicCursor = nextCursor
                .map(value -> PageRequestMapper.requireCursor(value, "nextCursor"))
                .orElse(null);
        return new PageResponse<>(items, publicCursor, publicCursor != null);
    }

    public <Item> PageResponse<Item> mapLookahead(
            List<Item> lookaheadItems,
            int limit,
            Function<Item, String> cursorFactory) {
        Objects.requireNonNull(lookaheadItems, "lookaheadItems");
        Objects.requireNonNull(cursorFactory, "cursorFactory");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (lookaheadItems.size() > limit + 1) {
            throw new IllegalArgumentException(
                    "lookahead items must contain at most limit + 1 entries");
        }
        if (lookaheadItems.size() <= limit) {
            return new PageResponse<>(lookaheadItems, null, false);
        }

        List<Item> visibleItems = List.copyOf(lookaheadItems.subList(0, limit));
        String nextCursor = PageRequestMapper.requireCursor(
                cursorFactory.apply(visibleItems.getLast()), "nextCursor");
        return new PageResponse<>(visibleItems, nextCursor, true);
    }

    public record PageResponse<Item>(
            List<Item> items,
            String nextCursor,
            boolean hasMore) {
        public PageResponse {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            if (hasMore != (nextCursor != null)) {
                throw new IllegalArgumentException(
                        "hasMore must match nextCursor presence");
            }
        }
    }
}
