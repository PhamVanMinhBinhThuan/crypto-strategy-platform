package com.cryptostrategy.platform.api.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PageMapperTest {
    private final PageRequestMapper requests = new PageRequestMapper();
    private final PageResponseMapper responses = new PageResponseMapper();

    @Test
    void requestUsesPublicDefaultsAndPreservesOpaqueCursor() {
        assertThat(requests.map(null, null))
                .isEqualTo(new PageRequestMapper.PageRequest(50, Optional.empty()));
        assertThat(requests.map(1, "opaque_CURSOR-1"))
                .isEqualTo(new PageRequestMapper.PageRequest(
                        1, Optional.of("opaque_CURSOR-1")));
        assertThat(requests.map(200, null).limit()).isEqualTo(200);
    }

    @Test
    void endpointCanChooseSmallerDefaultAndMaximumLimits() {
        assertThat(requests.map(null, null, 20, 100).limit()).isEqualTo(20);
        assertThat(requests.map(100, null, 20, 100).limit()).isEqualTo(100);

        assertThatThrownBy(() -> requests.map(101, null, 20, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 100");
    }

    @Test
    void invalidLimitsAndMalformedCursorsAreRejected() {
        assertThatThrownBy(() -> requests.map(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 200");
        assertThatThrownBy(() -> requests.map(201, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 200");

        for (String cursor : List.of(
                "",
                " ",
                " cursor",
                "cursor ",
                "cursor=",
                "cursor/value",
                "cửa-sổ",
                "a".repeat(1025))) {
            assertThatThrownBy(() -> requests.map(50, cursor))
                    .as("cursor %s", cursor)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cursor is malformed");
        }
    }

    @Test
    void responseMapsPublishedPortCursorAndCopiesItems() {
        List<String> mutable = new ArrayList<>(List.of("one", "two"));

        PageResponseMapper.PageResponse<String> page = responses.map(
                mutable, Optional.of("next_CURSOR-2"));
        mutable.add("three");

        assertThat(page.items()).containsExactly("one", "two");
        assertThat(page.nextCursor()).isEqualTo("next_CURSOR-2");
        assertThat(page.hasMore()).isTrue();
        assertThatThrownBy(() -> page.items().add("four"))
                .isInstanceOf(UnsupportedOperationException.class);

        PageResponseMapper.PageResponse<String> lastPage = responses.map(
                List.of("last"), Optional.empty());
        assertThat(lastPage.nextCursor()).isNull();
        assertThat(lastPage.hasMore()).isFalse();
    }

    @Test
    void lookaheadPageUsesLastVisibleItemAsContinuationBoundary() {
        PageResponseMapper.PageResponse<String> page = responses.mapLookahead(
                List.of("one", "two", "three", "four"),
                3,
                item -> "after-" + item);

        assertThat(page.items()).containsExactly("one", "two", "three");
        assertThat(page.nextCursor()).isEqualTo("after-three");
        assertThat(page.hasMore()).isTrue();

        PageResponseMapper.PageResponse<String> lastPage = responses.mapLookahead(
                List.of("one", "two"),
                3,
                item -> "after-" + item);
        assertThat(lastPage.items()).containsExactly("one", "two");
        assertThat(lastPage.nextCursor()).isNull();
        assertThat(lastPage.hasMore()).isFalse();
    }

    @Test
    void responseRejectsInvalidPortCursorAndUnboundedLookahead() {
        assertThatThrownBy(() -> responses.map(List.of("one"), Optional.of("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nextCursor is malformed");
        assertThatThrownBy(() -> responses.mapLookahead(
                        List.of("one", "two", "three"),
                        1,
                        item -> "after-" + item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lookahead items must contain at most limit + 1 entries");
    }
}
