package com.example;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private static final int MAX_AVATAR_CHARS = 500_000;

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** Creates the user on first login and refreshes their email and name on later logins. */
    @Transactional
    public User upsert(OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = token.getPrincipal();
        User user = userRepo.findByProviderAndProviderId(provider, token.getName())
                .orElseGet(() -> newUser(provider, token.getName(), principal));
        user.setEmail(principal.getAttribute("email"));
        user.setName(profileName(principal));
        return userRepo.save(user);
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

    private User newUser(String provider, String providerId, OAuth2User principal) {
        User user = new User(provider, providerId);
        String name = profileName(principal);
        String firstName = principal.getAttribute("given_name");
        user.setDisplayName(firstName != null ? firstName : name != null ? name : "Me");
        // Google calls the photo "picture"; GitHub calls it "avatar_url".
        String picture = principal.getAttribute("picture");
        user.setAvatar(picture != null ? picture : principal.getAttribute("avatar_url"));
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
