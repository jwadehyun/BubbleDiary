package com.example;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Two ways in. The mobile app sends "Authorization: Bearer <token>" (first chain); the web app uses the OAuth redirect
 * and a session cookie (second chain). Either way, every /api endpoint except sign-in needs a signed-in user.
 */
@Configuration
public class SecurityConfig {

    /** Requests from the mobile app: a bearer token, or the endpoints that hand one out. */
    private static final RequestMatcher APP_REQUESTS = new OrRequestMatcher(
            request -> {
                String header = request.getHeader("Authorization");
                return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
            },
            PathPatternRequestMatcher.withDefaults().matcher("/api/auth/token"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/auth/refresh"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/auth/logout"));

    @Bean
    @Order(1)
    SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(APP_REQUESTS)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                // Checks the token's signature and expiry with JwtConfig's decoder; a bad token gets a 401.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF attacks ride on cookies the browser sends by itself. A bearer header is never sent that way.
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webFilterChain(HttpSecurity http, UserService users) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is the front end's static files.
                        .anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            users.upsert((OAuth2AuthenticationToken) authentication);
                            response.sendRedirect("/");
                        })
                        .failureHandler((request, response, e) -> response.sendRedirect("/?loginError")))
                // The front end shows its own login screen, so API calls get a plain 401 instead of a redirect.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .requestCache(RequestCacheConfigurer::disable)
                .logout(logout -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                // XSRF-TOKEN cookie that the front end echoes back in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    /** The CSRF token is created lazily; load it on every request so the browser always has the cookie. */
    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
            chain.doFilter(request, response);
        }
    }
}
