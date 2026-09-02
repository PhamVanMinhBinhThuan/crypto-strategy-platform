package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/** Attaches the ticket-authenticated user as the WebSocket session principal. */
@Component
public final class WebSocketTicketHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes) {
        Object value = attributes.get(
                WebSocketTicketHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(value instanceof AuthenticatedUserContext user)) {
            throw new IllegalStateException("Authenticated WebSocket user is missing");
        }
        return new RealtimePrincipal(user);
    }

    public record RealtimePrincipal(AuthenticatedUserContext user) implements Principal {
        @Override
        public String getName() {
            return user.userId().toString();
        }
    }
}
