package com.cryptostrategy.platform.api.support;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** Capturing test doubles adaptable to published single-input application ports. */
public final class FakePublishedPorts {
    private FakePublishedPorts() {}

    public static <I, O> CapturingPort<I, O> respondingWith(
            Function<? super I, ? extends O> response) {
        return new CapturingPort<>(response);
    }

    public static <I, O> CapturingPort<I, O> returning(O response) {
        return respondingWith(ignored -> response);
    }

    public static final class CapturingPort<I, O> {
        private final Function<? super I, ? extends O> response;
        private final CopyOnWriteArrayList<I> invocations = new CopyOnWriteArrayList<>();

        private CapturingPort(Function<? super I, ? extends O> response) {
            this.response = Objects.requireNonNull(response, "response");
        }

        public O invoke(I input) {
            invocations.add(input);
            return response.apply(input);
        }

        public List<I> invocations() {
            return List.copyOf(invocations);
        }

        public void reset() {
            invocations.clear();
        }
    }
}
