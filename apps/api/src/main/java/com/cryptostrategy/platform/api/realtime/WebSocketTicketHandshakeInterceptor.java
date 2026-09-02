package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.api.auth.AllowedOriginPolicy;
import com.cryptostrategy.platform.api.auth.WebSocketTicketService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/** Authenticates a WebSocket upgrade without exposing a long-lived browser token. */
@Component
public final class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {
    static final String AUTHENTICATED_USER_ATTRIBUTE =
            WebSocketTicketHandshakeInterceptor.class.getName() + ".authenticatedUser";
    private static final String ERROR_CODE_HEADER = "X-Error-Code";

    private final WebSocketTicketService tickets;
    private final AllowedOriginPolicy origins;

    public WebSocketTicketHandshakeInterceptor(
            WebSocketTicketService tickets, AllowedOriginPolicy origins) {
        this.tickets = tickets;
        this.origins = origins;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes) {
        String origin;
        try {
            origin = origins.requireAllowed(request.getHeaders().getOrigin());
        } catch (AccessDeniedException exception) {
            reject(response, HttpStatus.FORBIDDEN, "FORBIDDEN_ORIGIN");
            return false;
        }

        try {
            String ticket = requireSingleTicket(request);
            var consumed = tickets.consumeForHandshake(ticket, origin);
            attributes.put(AUTHENTICATED_USER_ATTRIBUTE, consumed.user());
            return true;
        } catch (IllegalArgumentException exception) {
            reject(response, HttpStatus.UNAUTHORIZED, "WEBSOCKET_TICKET_INVALID");
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Exception exception) {
        // Không ghi request URI vì query chứa one-time ticket.
    }

    private static String requireSingleTicket(ServerHttpRequest request) {
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();
        List<String> values = query.get("ticket");
        if (values == null || values.size() != 1 || query.size() != 1) {
            throw new IllegalArgumentException("A single WebSocket ticket is required");
        }
        return values.getFirst();
    }

    private static void reject(
            ServerHttpResponse response, HttpStatus status, String safeCode) {
        response.setStatusCode(status);
        response.getHeaders().set(ERROR_CODE_HEADER, safeCode);
    }
}
