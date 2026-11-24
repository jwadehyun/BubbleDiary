package com.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Fills in {@link CurrentUser} parameters. */
@Configuration
public class CurrentUserResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {
    private final UserService users;

    public CurrentUserResolver(UserService users) {
        this.users = users;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType() == User.class;
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest request,
                                WebDataBinderFactory binders) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Web: signed in through the OAuth redirect, kept in a session cookie.
        if (auth instanceof OAuth2AuthenticationToken web) return users.require(web);
        // App: a bearer token this server issued, whose subject is the user id.
        if (auth instanceof JwtAuthenticationToken app) {
            return users.byId(Long.valueOf(app.getName()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account no longer exists"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
