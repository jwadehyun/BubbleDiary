package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Sign-in for the mobile app, which uses bearer tokens instead of the web's session cookie. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IdTokenVerifier verifier;
    private final UserService users;
    private final TokenService tokens;

    public AuthController(IdTokenVerifier verifier, UserService users, TokenService tokens) {
        this.verifier = verifier;
        this.users = users;
        this.tokens = tokens;
    }

    /**
     * provider: "google" or "apple". idToken: from native sign-in on the phone. name: Apple only shares the user's
     * name with the app on the first sign-in, not in the token, so the app passes it along.
     */
    public record TokenRequest(String provider, String idToken, String name) {}

    public record RefreshRequest(String refreshToken) {}

    // POST /api/auth/token: trade a Google/Apple ID token for our own tokens
    @PostMapping("/token")
    public TokenService.Tokens signIn(@RequestBody TokenRequest request) {
        LoginIdentity id = verifier.verify(request.provider(), request.idToken());
        if (id.name() == null && request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            id = new LoginIdentity(id.provider(), id.subject(), id.email(), name, name.split("\\s+")[0], id.picture());
        }
        return tokens.issue(users.upsert(id));
    }

    // POST /api/auth/refresh: new tokens when the access token expires
    @PostMapping("/refresh")
    public TokenService.Tokens refresh(@RequestBody RefreshRequest request) {
        return tokens.refresh(request.refreshToken());
    }

    // POST /api/auth/logout: the refresh token stops working
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        tokens.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
