package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mobile app sign-in with bearer tokens. Google itself is faked, since tests can't sign in to a real account. */
@SpringBootTest
@AutoConfigureMockMvc
class AppAuthTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @MockitoBean
    IdTokenVerifier verifier;

    private String sub;

    @BeforeEach
    void fakeGoogle() {
        sub = UUID.randomUUID().toString();
        when(verifier.verify("google", "good-id-token"))
                .thenReturn(new LoginIdentity("google", sub, "jisung@example.com", "Han Jisung", "Jisung", null));
        when(verifier.verify(anyString(), eq("forged-id-token")))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign-in token is invalid or expired"));
    }

    private JsonNode signIn() throws Exception {
        return body(mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"google\",\"idToken\":\"good-id-token\"}"))
                .andExpect(status().isOk()));
    }

    private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, JsonNode tokens) {
        return request.header("Authorization", "Bearer " + tokens.path("accessToken").asString());
    }

    @Test
    void signInGivesTokensThatWorkWithoutCsrf() throws Exception {
        JsonNode tokens = signIn();
        assertThat(tokens.path("expiresIn").asLong()).isEqualTo(3600);

        mvc.perform(bearer(get("/api/me"), tokens))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jisung@example.com"))
                .andExpect(jsonPath("$.settings.displayName").value("Jisung"));
        // No CSRF token needed: the app isn't a browser carrying cookies.
        mvc.perform(bearer(post("/api/messages"), tokens).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hi from my phone\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsForgedIdTokensAndBadBearerTokens() throws Exception {
        mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"google\",\"idToken\":\"forged-id-token\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devSignInIsOffByDefault() throws Exception {
        mvc.perform(post("/api/auth/dev").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"anyone\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshTokensWorkOnce() throws Exception {
        JsonNode first = signIn();
        String refresh = "{\"refreshToken\":\"" + first.path("refreshToken").asString() + "\"}";

        JsonNode second = body(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refresh))
                .andExpect(status().isOk()));
        mvc.perform(bearer(get("/api/me"), second)).andExpect(status().isOk());
        assertThat(second.path("refreshToken").asString()).isNotEqualTo(first.path("refreshToken").asString());

        // A stolen refresh token that was already used gets nothing.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheRefreshToken() throws Exception {
        String refresh = "{\"refreshToken\":\"" + signIn().path("refreshToken").asString() + "\"}";
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refresh))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webAndAppShareOneAccount() throws Exception {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId("test-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .build();
        var web = oauth2Login().clientRegistration(google).attributes(a -> a.put("sub", sub));
        mvc.perform(post("/api/messages").with(web).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"posted on the web\"}"))
                .andExpect(status().isOk());

        mvc.perform(bearer(get("/api/messages"), signIn()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("posted on the web"));
    }

    @Test
    void deletingTheAccountRemovesEverything() throws Exception {
        JsonNode tokens = signIn();
        mvc.perform(bearer(post("/api/messages"), tokens).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"bye\"}")).andExpect(status().isOk());

        mvc.perform(bearer(delete("/api/me"), tokens)).andExpect(status().isNoContent());

        // The access token is still validly signed, but its user is gone.
        mvc.perform(bearer(get("/api/me"), tokens)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.path("refreshToken").asString() + "\"}"))
                .andExpect(status().isUnauthorized());
        // Signing in again starts a fresh, empty account.
        mvc.perform(bearer(get("/api/messages"), signIn())).andExpect(jsonPath("$.length()").value(0));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
