package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens for the mobile app. The access token is short-lived and checked without the database; the refresh token
 * lasts a month, is stored hashed, and is swapped for a new one on every use.
 */
@Service
public class TokenService {
    static final String ISSUER = "bubblediary";
    static final Duration ACCESS_TTL = Duration.ofHours(1);
    static final Duration REFRESH_TTL = Duration.ofDays(30);

    public record Tokens(String accessToken, String refreshToken, long expiresIn) {}

    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public Tokens issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TTL))
                .build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user, hash(refresh), now.plus(REFRESH_TTL)));
        return new Tokens(access, refresh, ACCESS_TTL.toSeconds());
    }

    /** Trades a refresh token for new tokens. Each refresh token works once. */
    @Transactional
    public Tokens refresh(String refreshToken) {
        String hash = hash(refreshToken);
        RefreshToken stored = refreshTokens.findValid(hash, Instant.now()).orElseThrow(TokenService::signInAgain);
        // Only one of two simultaneous refreshes gets to delete it, so a token can't be spent twice.
        if (refreshTokens.deleteByHash(hash) == 0) throw signInAgain();
        return issue(stored.getUser());
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokens.deleteByHash(hash(refreshToken));
    }

    private static ResponseStatusException signInAgain() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again");
    }

    private static String hash(String token) {
        if (token == null || token.isBlank()) throw signInAgain();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
