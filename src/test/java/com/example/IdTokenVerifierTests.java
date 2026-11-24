package com.example;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The checks on Google/Apple ID tokens, using a key made up for the test in place of Google's. The signature check
 * is the same code either way; only where the public key comes from differs.
 */
class IdTokenVerifierTests {
    private static final String ISSUER = "https://accounts.google.com";
    private static final String OUR_APP = "our-client-id.apps.googleusercontent.com";

    private final JwtEncoder google;
    private final IdTokenVerifier verifier;

    IdTokenVerifierTests() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        RSAKey key = new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey((RSAPrivateKey) keys.getPrivate()).build();
        google = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        decoder.setJwtValidator(IdTokenVerifier.validator(Set.of(ISSUER), List.of(OUR_APP)));
        verifier = new IdTokenVerifier(Map.of("google", decoder));
    }

    private String token(String issuer, String audience, Instant expires) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("google-user-123")
                .claim("email", "jisung@example.com")
                .claim("given_name", "Jisung")
                .issuedAt(expires.minusSeconds(3600))
                .expiresAt(expires)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return google.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static void assertRejected(Runnable verify, HttpStatus status) {
        assertThatThrownBy(verify::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    @Test
    void acceptsAValidToken() {
        LoginIdentity id = verifier.verify("google", token(ISSUER, OUR_APP, Instant.now().plusSeconds(600)));
        assertThat(id).isEqualTo(new LoginIdentity("google", "google-user-123", "jisung@example.com", null, "Jisung", null));
    }

    @Test
    void rejectsATokenMadeForAnotherApp() {
        String other = token(ISSUER, "someone-elses-app", Instant.now().plusSeconds(600));
        assertRejected(() -> verifier.verify("google", other), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAWrongIssuer() {
        String fake = token("https://evil.example.com", OUR_APP, Instant.now().plusSeconds(600));
        assertRejected(() -> verifier.verify("google", fake), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAnExpiredToken() {
        String old = token(ISSUER, OUR_APP, Instant.now().minusSeconds(3600));
        assertRejected(() -> verifier.verify("google", old), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsGarbageAndUnknownProviders() {
        assertRejected(() -> verifier.verify("google", "not.a.jwt"), HttpStatus.UNAUTHORIZED);
        assertRejected(() -> verifier.verify("myspace", "anything"), HttpStatus.BAD_REQUEST);
    }
}
