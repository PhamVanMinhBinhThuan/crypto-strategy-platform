package com.cryptostrategy.platform.api.auth;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/realtime")
public final class WebSocketTicketController {
    private final WebSocketTicketService tickets;
    private final AllowedOriginPolicy origins;

    public WebSocketTicketController(
            WebSocketTicketService tickets,
            AllowedOriginPolicy origins) {
        this.tickets = tickets;
        this.origins = origins;
    }

    @PostMapping("/ticket")
    public ResponseEntity<TicketResponse> issue(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader(name = "Origin", required = false) String origin) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String allowedOrigin = origins.requireAllowed(origin);
        WebSocketTicketService.IssuedTicket issued = tickets.issue(
                user.userId(), allowedOrigin, user.authenticationExpiresAt());
        return ResponseEntity.ok(new TicketResponse(issued.ticket(), issued.expiresAt()));
    }

    public record TicketResponse(String ticket, Instant expiresAt) {}
}
