package com.example;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService users;
    private final OpenRouterClient openRouter;
    private final ClientRegistrationRepository registrations;

    public UserController(UserService users, OpenRouterClient openRouter, ClientRegistrationRepository registrations) {
        this.users = users;
        this.openRouter = openRouter;
        this.registrations = registrations;
    }

    public record Provider(String id, String name, String loginUrl) {}

    public record Me(String email, String name, boolean aiEnabled, String plan, DiarySettings settings) {
        static Me of(User user, boolean aiEnabled) {
            return new Me(user.getEmail(), user.getName(), aiEnabled, user.getPlan().json(), DiarySettings.of(user));
        }
    }

    // GET /api/auth/providers: the login buttons to show (public)
    @GetMapping("/auth/providers")
    public List<Provider> getProviders() {
        List<Provider> providers = new ArrayList<>();
        if (registrations instanceof Iterable<?> all) {
            for (Object o : all) {
                ClientRegistration r = (ClientRegistration) o;
                providers.add(new Provider(r.getRegistrationId(), r.getClientName(),
                        "/oauth2/authorization/" + r.getRegistrationId()));
            }
        }
        return providers;
    }

    // GET /api/me
    @GetMapping("/me")
    public Me getMe(@CurrentUser User user) {
        return Me.of(user, openRouter.isConfigured());
    }

    // PUT /api/me/settings
    @PutMapping("/me/settings")
    public Me updateSettings(@CurrentUser User user, @RequestBody DiarySettings settings) {
        return Me.of(users.updateSettings(user, settings), openRouter.isConfigured());
    }

    // DELETE /api/me: deletes the account and all its posts, then signs out
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@CurrentUser User user, HttpServletRequest request) throws ServletException {
        users.deleteAccount(user);
        request.logout();
        return ResponseEntity.noContent().build();
    }
}
