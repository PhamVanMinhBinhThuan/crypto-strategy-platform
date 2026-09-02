package com.cryptostrategy.platform.api.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import org.junit.jupiter.api.Test;

class ApiTestSupportTest {
    @Test
    void exposesStableDistinctAuthenticatedUsers() {
        assertThat(AuthenticatedUsers.userA().userId())
                .isEqualTo(AuthenticatedUsers.USER_A_ID)
                .isNotEqualTo(AuthenticatedUsers.userB().userId());
    }

    @Test
    void createsCanonicalOpaqueIdsAndSafeCorrelationIds() {
        assertThat(Ulids.requireValid(TestIdentifiers.opaqueId(42)))
                .isEqualTo(TestIdentifiers.opaqueId(42));
        assertThat(TestIdentifiers.correlationId("MARKET-CONTRACT"))
                .isEqualTo("F009-MARKET-CONTRACT");
        assertThatThrownBy(() -> TestIdentifiers.correlationId("market contract"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void capturesPublishedPortInvocationsWithoutSharingMutableHistory() {
        var port = FakePublishedPorts.respondingWith(String::length);

        assertThat(port.invoke("dataset-request")).isEqualTo(15);
        assertThat(port.invocations()).containsExactly("dataset-request");
        assertThatThrownBy(() -> port.invocations().add("another-request"))
                .isInstanceOf(UnsupportedOperationException.class);

        port.reset();
        assertThat(port.invocations()).isEmpty();
    }
}
