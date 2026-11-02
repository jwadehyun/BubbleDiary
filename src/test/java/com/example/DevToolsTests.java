package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Settings only for developing the mobile app: sign-in by name, and letting its web preview call the server. */
@SpringBootTest(properties = {"app.auth.dev-login=true", "app.cors.allowed-origins=http://localhost:8081"})
@AutoConfigureMockMvc
class DevToolsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Test
    void signsInByNameWhenTurnedOn() throws Exception {
        String body = mvc.perform(post("/api/auth/dev").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Han Jisung\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).path("accessToken").asString();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.displayName").value("Han Jisung"));
    }

    @Test
    void webPreviewMayCallTheApiButOtherSitesMayNot() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
        mvc.perform(options("/api/me").header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
