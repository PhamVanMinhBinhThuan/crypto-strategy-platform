package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.auth.AuthenticationFailureHandler;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${platform.security.jwt.issuer:}") String issuer,
            @Value("${platform.security.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${platform.security.jwt.audience:}") String audience) {
        String requiredIssuer = requireNonBlank("SUPABASE_JWT_ISSUER", issuer);
        String requiredJwkSetUri = requireNonBlank("SUPABASE_JWT_JWKS_URI", jwkSetUri);
        String requiredAudience = requireNonBlank("SUPABASE_JWT_AUDIENCE", audience);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(requiredJwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(requiredIssuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", audiences -> audiences != null && audiences.contains(requiredAudience));
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> validUuidSubject(jwt.getSubject());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerAndTime,
                audienceValidator,
                subjectValidator));
        return decoder;
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> authenticatedUserConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        return jwt -> new UserContextAuthenticationToken(
                jwt,
                authoritiesConverter.convert(jwt),
                AuthenticatedUserContext.fromSubject(jwt.getSubject()));
    }

    @Bean
    @Order(0)
    SecurityFilterChain internalServiceSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/internal/news-items/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            AuthenticationFailureHandler authenticationFailureHandler,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authenticatedUserConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationFailureHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticatedUserConverter)))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationFailureHandler));
        return http.build();
    }

    private static OAuth2TokenValidatorResult validUuidSubject(String subject) {
        try {
            AuthenticatedUserContext.fromSubject(subject);
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException exception) {
            OAuth2Error error = new OAuth2Error("invalid_token", "JWT subject must be a UUID", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }

    private static String requireNonBlank(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key);
        }
        return value;
    }

    @SuppressWarnings("serial")
    private static final class UserContextAuthenticationToken extends JwtAuthenticationToken {
        private final AuthenticatedUserContext userContext;

        private UserContextAuthenticationToken(
                Jwt jwt,
                Collection<GrantedAuthority> authorities,
                AuthenticatedUserContext userContext) {
            super(jwt, authorities, userContext.userId().toString());
            this.userContext = userContext;
        }

        @Override
        public Object getPrincipal() {
            return userContext;
        }
    }
}
