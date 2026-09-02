package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.api.auth.AllowedOriginPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final RealtimeConnection connection;
    private final WebSocketTicketHandshakeInterceptor ticketHandshake;
    private final WebSocketTicketHandshakeHandler handshakeHandler;
    private final AllowedOriginPolicy origins;

    public WebSocketConfiguration(
            RealtimeConnection connection,
            WebSocketTicketHandshakeInterceptor ticketHandshake,
            WebSocketTicketHandshakeHandler handshakeHandler,
            AllowedOriginPolicy origins) {
        this.connection = connection;
        this.ticketHandshake = ticketHandshake;
        this.handshakeHandler = handshakeHandler;
        this.origins = origins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(connection, "/ws")
                .addInterceptors(ticketHandshake)
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOrigins(origins.allowedOrigins().toArray(String[]::new));
    }
}
