package com.cryptostrategy.platform.persistence.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchRunStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SearchPersistenceFactoryTest {
    @Test
    void exposesSearchStoreOnlyThroughPublishedPort() {
        var factory = new SearchPersistenceFactory(mock(JdbcTemplate.class));
        assertThat(factory.createSearchRunStore()).isInstanceOf(JdbcSearchRunStore.class);
    }
}
