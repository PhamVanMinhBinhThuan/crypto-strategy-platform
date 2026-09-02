package com.cryptostrategy.platform.api.realtime;

import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.api.auth.WebSocketTicketService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:websocket-ticket;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated",
            "platform.security.allowed-origins=https://dashboard.example.test,https://preview.example.test",
            "platform.security.websocket-ticket-lifetime=PT1S",
            "platform.security.websocket-max-connection-lifetime=PT5S"
        })
class WebSocketTicketIntegrationTest {
    private static final String ALLOWED_ORIGIN = "https://dashboard.example.test";
    private static final String OTHER_ALLOWED_ORIGIN = "https://preview.example.test";
    private static final String FORBIDDEN_ORIGIN = "https://evil.example.test";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketTicketService tickets;

    @Test
    void successfulHandshakeConsumesTicketExactlyOnce() {
        var issued = tickets.issue(
                USER_A_ID, ALLOWED_ORIGIN, Instant.now().plus(Duration.ofMinutes(5)));

        ConnectedClient first = connect(issued.ticket(), ALLOWED_ORIGIN);
        first.closeNormally();

        assertThat(rejectedHandshake(issued.ticket(), ALLOWED_ORIGIN))
                .isEqualTo(new RejectedHandshake(401, "WEBSOCKET_TICKET_INVALID"));
    }

    @Test
    void expiredTicketIsRejectedWithoutRevealingItsState() throws InterruptedException {
        var issued = tickets.issue(
                USER_A_ID, ALLOWED_ORIGIN, Instant.now().plus(Duration.ofMinutes(5)));
        long waitMillis = Math.max(
                1L, Duration.between(Instant.now(), issued.expiresAt()).toMillis() + 50L);
        TimeUnit.MILLISECONDS.sleep(waitMillis);

        assertThat(rejectedHandshake(issued.ticket(), ALLOWED_ORIGIN))
                .isEqualTo(new RejectedHandshake(401, "WEBSOCKET_TICKET_INVALID"));
    }

    @Test
    void handshakeRequiresTheTicketOriginAndConfiguredAllowlist() {
        var wrongBinding = tickets.issue(
                USER_A_ID, ALLOWED_ORIGIN, Instant.now().plus(Duration.ofMinutes(5)));
        assertThat(rejectedHandshake(wrongBinding.ticket(), OTHER_ALLOWED_ORIGIN))
                .isEqualTo(new RejectedHandshake(401, "WEBSOCKET_TICKET_INVALID"));

        var forbidden = tickets.issue(
                USER_A_ID, ALLOWED_ORIGIN, Instant.now().plus(Duration.ofMinutes(5)));
        assertThat(rejectedHandshake(forbidden.ticket(), FORBIDDEN_ORIGIN))
                .isEqualTo(new RejectedHandshake(403, "FORBIDDEN_ORIGIN"));
    }

    @Test
    void authenticationExpiryClosesPrivateConnectionAndRequiresFreshTicket() {
        var issued = tickets.issue(
                USER_A_ID, ALLOWED_ORIGIN, Instant.now().plus(Duration.ofSeconds(1)));

        ConnectedClient connection = connect(issued.ticket(), ALLOWED_ORIGIN);

        CloseFrame close = connection.closed().orTimeout(3, TimeUnit.SECONDS).join();
        assertThat(close.code()).isEqualTo(4001);
        assertThat(close.reason()).isEqualTo("REAUTHENTICATION_REQUIRED");
    }

    private ConnectedClient connect(String ticket, String origin) {
        ClientListener listener = new ClientListener();
        WebSocket socket = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .header("Origin", origin)
                .buildAsync(webSocketUri(ticket), listener)
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
        return new ConnectedClient(socket, listener.closed);
    }

    private RejectedHandshake rejectedHandshake(String ticket, String origin) {
        try {
            connect(ticket, origin).closeNormally();
            throw new AssertionError("Expected WebSocket handshake rejection");
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof WebSocketHandshakeException handshake) {
                String code = handshake.getResponse()
                        .headers()
                        .firstValue("X-Error-Code")
                        .orElseThrow();
                assertThat(handshake.getResponse().headers().map().values())
                        .allSatisfy(values -> assertThat(values)
                                .noneMatch(value -> value.contains(ticket)));
                return new RejectedHandshake(handshake.getResponse().statusCode(), code);
            }
            throw exception;
        }
    }

    private URI webSocketUri(String ticket) {
        return URI.create("ws://localhost:" + port + "/ws?ticket=" + ticket);
    }

    private record ConnectedClient(WebSocket socket, CompletableFuture<CloseFrame> closed) {
        void closeNormally() {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "TEST_COMPLETE")
                    .orTimeout(3, TimeUnit.SECONDS)
                    .join();
        }
    }

    private record CloseFrame(int code, String reason) {}

    private record RejectedHandshake(int status, String code) {}

    private static final class ClientListener implements WebSocket.Listener {
        private final CompletableFuture<CloseFrame> closed = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(new CloseFrame(statusCode, reason));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.completeExceptionally(error);
        }
    }
}
