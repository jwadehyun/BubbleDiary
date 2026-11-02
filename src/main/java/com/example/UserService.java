package com.example;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {
    private static final int MAX_AVATAR_CHARS = 500_000;

    private final UserRepository userRepo;
    private final MessageRepository messageRepo;
    private final RefreshTokenRepository refreshTokens;

    public UserService(UserRepository userRepo, MessageRepository messageRepo, RefreshTokenRepository refreshTokens) {
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
        this.refreshTokens = refreshTokens;
    }

    /** Web sign-in. */
    @Transactional
    public User upsert(OAuth2AuthenticationToken token) {
        OAuth2User principal = token.getPrincipal();
        // Google calls the photo "picture"; GitHub calls it "avatar_url".
        String picture = principal.getAttribute("picture");
        return upsert(new LoginIdentity(token.getAuthorizedClientRegistrationId(), token.getName(),
                principal.getAttribute("email"), profileName(principal), principal.getAttribute("given_name"),
                picture != null ? picture : principal.getAttribute("avatar_url")));
    }

    /**
     * Creates the user on first sign-in and refreshes their email and name on later ones. Web and app sign-ins with
     * the same Google account land on the same user, since both are keyed by Google's id for them.
     */
    @Transactional
    public User upsert(LoginIdentity id) {
        User user = userRepo.findByProviderAndProviderId(id.provider(), id.subject()).orElseGet(() -> newUser(id));
        // Apple only sends the name on the very first sign-in, so don't wipe what we have.
        if (id.email() != null) user.setEmail(id.email());
        if (id.name() != null) user.setName(id.name());
        return userRepo.save(user);
    }

    public Optional<User> byId(Long id) {
        return userRepo.findById(id);
    }

    /** The signed-in user. Creates them if missing, e.g. after the database was reset. */
    public User require(OAuth2AuthenticationToken token) {
        if (token == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userRepo.findByProviderAndProviderId(token.getAuthorizedClientRegistrationId(), token.getName())
                .orElseGet(() -> {
                    try {
                        return upsert(token);
                    } catch (DataIntegrityViolationException e) {
                        // A parallel request created them first.
                        return userRepo.findByProviderAndProviderId(
                                token.getAuthorizedClientRegistrationId(), token.getName()).orElseThrow();
                    }
                });
    }

    @Transactional
    public User updateSettings(User user, DiarySettings s) {
        if (s.displayName() != null) user.setDisplayName(clip(s.displayName(), 40, user.getDisplayName()));
        if (s.fandom() != null) user.setFandom(clip(s.fandom(), 40, user.getFandom()));
        if (s.avatar() != null) {
            if (s.avatar().length() > MAX_AVATAR_CHARS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile photo is too large");
            }
            user.setAvatar(s.avatar().isBlank() ? null : s.avatar());
        }
        if (s.statusText() != null) user.setStatusText(optional(s.statusText(), 60));
        if (s.profileMessage() != null) user.setProfileMessage(optional(s.profileMessage(), 150));
        if (s.fanPrompt() != null) user.setFanPrompt(optional(s.fanPrompt(), 300));
        if (s.voice() != null) {
            try {
                user.setVoice(FanVoice.parse(s.voice()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown fan voice: " + s.voice());
            }
        }
        return userRepo.save(user);
    }

    /** Deletes the user and everything they posted. App stores require this to be possible from the app. */
    @Transactional
    public void deleteAccount(User user) {
        messageRepo.deleteAllOwned(user);
        refreshTokens.deleteAllOwned(user);
        userRepo.deleteById(user.getId());
    }

    private static User newUser(LoginIdentity id) {
        User user = new User(id.provider(), id.subject());
        user.setDisplayName(id.givenName() != null ? id.givenName() : id.name() != null ? id.name() : "Me");
        user.setAvatar(id.picture());
        return user;
    }

    private static String profileName(OAuth2User principal) {
        String name = principal.getAttribute("name");
        return name != null ? name : principal.getAttribute("login");
    }

    private static String clip(String value, int max, String fallback) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return fallback;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /** Text the user may leave empty; blank clears it. */
    private static String optional(String value, int max) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
