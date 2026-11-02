package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Checks the ID token the mobile app got from native Google or Apple sign-in: signed by Google/Apple, not expired,
 * and issued for this app. Without this check anyone could claim to be anyone.
 */
@Component
public class IdTokenVerifier {
    private static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String APPLE_KEYS = "https://appleid.apple.com/auth/keys";
    private static final Set<String> APPLE_ISSUERS = Set.of("https://appleid.apple.com");

    private final Map<String, JwtDecoder> decoders;

    /**
     * googleClientIds: the OAuth client ids the app signs in with (the web client id, which native Google sign-in
     * puts in the token, plus any iOS/Android ones). appleClientIds: the iOS bundle id.
     */
    @Autowired
    public IdTokenVerifier(@Value("${app.auth.google-client-ids:}") String googleClientIds,
                           @Value("${app.auth.apple-client-ids:}") String appleClientIds) {
        Map<String, JwtDecoder> decoders = new HashMap<>();
        List<String> google = split(googleClientIds);
        List<String> apple = split(appleClientIds);
        if (!google.isEmpty()) decoders.put("google", decoder(GOOGLE_KEYS, GOOGLE_ISSUERS, google));
        if (!apple.isEmpty()) decoders.put("apple", decoder(APPLE_KEYS, APPLE_ISSUERS, apple));
        this.decoders = Map.copyOf(decoders);
    }

    IdTokenVerifier(Map<String, JwtDecoder> decoders) {
        this.decoders = Map.copyOf(decoders);
    }

    public LoginIdentity verify(String provider, String idToken) {
        JwtDecoder decoder = provider == null ? null : decoders.get(provider);
        if (decoder == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sign-in with " + provider + " isn't set up on this server");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign-in token is invalid or expired");
        }
        return new LoginIdentity(provider, jwt.getSubject(), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"), jwt.getClaimAsString("given_name"), jwt.getClaimAsString("picture"));
    }

    static OAuth2TokenValidator<Jwt> validator(Set<String> issuers, Collection<String> audiences) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> iss != null && issuers.contains(iss.toString())),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.stream().anyMatch(audiences::contains)));
    }

    private static JwtDecoder decoder(String keysUrl, Set<String> issuers, List<String> audiences) {
        // The signing keys are downloaded on first use and cached.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keysUrl).build();
        decoder.setJwtValidator(validator(issuers, audiences));
        return decoder;
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
