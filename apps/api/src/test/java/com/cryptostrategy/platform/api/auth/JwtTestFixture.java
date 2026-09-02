package com.cryptostrategy.platform.api.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

final class JwtTestFixture implements AutoCloseable {
    static final String ISSUER = "https://fixture.supabase.invalid/auth/v1";
    static final String AUDIENCE = "authenticated";

    private static final String KEY_ID = "fixture-key";

    private final KeyPair signingKey;
    private final KeyPair invalidSigningKey;
    private final HttpServer jwksServer;

    private JwtTestFixture(KeyPair signingKey, KeyPair invalidSigningKey, HttpServer jwksServer) {
        this.signingKey = signingKey;
        this.invalidSigningKey = invalidSigningKey;
        this.jwksServer = jwksServer;
    }

    static JwtTestFixture start() {
        try {
            KeyPair signingKey = generateKeyPair();
            KeyPair invalidSigningKey = generateKeyPair();
            RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) signingKey.getPublic())
                    .keyID(KEY_ID)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .build();
            byte[] body = ("{\"keys\":[" + publicJwk.toPublicJWK() + "]}")
                    .getBytes(StandardCharsets.UTF_8);

            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return new JwtTestFixture(signingKey, invalidSigningKey, server);
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not create local JWT fixture", exception);
        }
    }

    String jwksUri() {
        return "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks";
    }

    String validToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String invalidSignatureToken(UUID userId) {
        return token(invalidSigningKey, KEY_ID, userId.toString(), ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String unknownKeyToken(UUID userId) {
        return token(signingKey, "unknown-key", userId.toString(), ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String expiredToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, List.of(AUDIENCE),
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(300));
    }

    String notYetValidToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(600), Instant.now().plusSeconds(300));
    }

    String wrongIssuerToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), "https://wrong.invalid", List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String wrongAudienceToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, List.of("wrong-audience"),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String missingAudienceToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, null,
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String nonUuidSubjectToken() {
        return token(signingKey, KEY_ID, "not-a-uuid", ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String nonCanonicalUuidSubjectToken() {
        return token(signingKey, KEY_ID, "1-1-1-1-1", ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String missingSubjectToken() {
        return token(signingKey, KEY_ID, null, ISSUER, List.of(AUDIENCE),
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
    }

    String missingExpirationToken(UUID userId) {
        return token(signingKey, KEY_ID, userId.toString(), ISSUER, List.of(AUDIENCE),
                null, Instant.now().minusSeconds(5));
    }

    private static String token(
            KeyPair keyPair,
            String keyId,
            String subject,
            String issuer,
            List<String> audience,
            Instant expiresAt,
            Instant notBefore) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(Instant.now()))
                    .notBeforeTime(Date.from(notBefore));
            if (expiresAt != null) {
                claims.expirationTime(Date.from(expiresAt));
            }
            if (subject != null) {
                claims.subject(subject);
            }
            if (audience != null) {
                claims.audience(audience);
            }
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                    claims.build());
            jwt.sign(new RSASSASigner(keyPair.getPrivate()));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign local JWT fixture", exception);
        }
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Override
    public void close() {
        jwksServer.stop(0);
    }
}
