package com.example;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in without Google or Apple, for running the app in Expo Go or a simulator before real sign-in is set up.
 * Anyone who can reach the server could sign in as anyone, so this only exists when DEV_LOGIN=true.
 * Never turn it on in production.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.auth.dev-login", havingValue = "true")
public class DevLoginController {
    private static final Logger log = LoggerFactory.getLogger(DevLoginController.class);

    private final UserService users;
    private final TokenService tokens;

    public DevLoginController(UserService users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public record DevLoginRequest(String name) {}

    @PostConstruct
    void warn() {
        log.warn("Dev sign-in is ON (DEV_LOGIN=true): anyone can sign in as anyone. Never use this in production.");
    }

    // POST /api/auth/dev: signs in as a test user with this name, creating them the first time
    @PostMapping("/dev")
    public TokenService.Tokens signIn(@RequestBody DevLoginRequest request) {
        String name = request.name() == null || request.name().isBlank() ? "Dev" : request.name().trim();
        if (name.length() > 40) name = name.substring(0, 40);
        String subject = name.toLowerCase().replaceAll("[^\\p{L}\\p{N}]+", "-");
        return tokens.issue(users.upsert(new LoginIdentity("dev", subject, null, name, name, null)));
    }
}
