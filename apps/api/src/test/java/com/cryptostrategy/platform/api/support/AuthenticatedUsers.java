package com.cryptostrategy.platform.api.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Stable authenticated identities shared by F-009 boundary tests. */
public final class AuthenticatedUsers {
    public static final UUID USER_A_ID =
            UUID.fromString("d1203948-8ff9-4916-9964-fecbed13d4db");
    public static final UUID USER_B_ID =
            UUID.fromString("9a3b2b5e-6e60-494d-b62e-e576e31361ad");

    private AuthenticatedUsers() {}

    public static AuthenticatedUserContext userA() {
        return new AuthenticatedUserContext(USER_A_ID);
    }

    public static AuthenticatedUserContext userB() {
        return new AuthenticatedUserContext(USER_B_ID);
    }

    public static RequestPostProcessor authenticatedAs(UUID userId) {
        var token = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserContext(userId),
                "test-credential",
                List.of());
        return authentication(token);
    }
}
