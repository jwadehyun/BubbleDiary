package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BubbleDiaryApplicationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    ReplyGenerator generator;

    static RequestPostProcessor user(String sub) {
        return oauth2Login().attributes(a -> {
            a.put("sub", sub);
            a.put("name", "Han Jisung");
            a.put("given_name", "Jisung");
            a.put("email", sub + "@example.com");
        });
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/messages")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsLoginProviders() throws Exception {
        mvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("google"))
                .andExpect(jsonPath("$[0].loginUrl").value("/oauth2/authorization/google"));
    }

    @Test
    void newUserGetsSettingsFromProfile() throws Exception {
        mvc.perform(get("/api/me").with(user(UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Han Jisung"))
                .andExpect(jsonPath("$.aiEnabled").value(false))
                .andExpect(jsonPath("$.settings.displayName").value("Jisung"))
                .andExpect(jsonPath("$.settings.fandom").value("Bubblies"))
                .andExpect(jsonPath("$.settings.voice").value("english"));
    }

    @Test
    void writesRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/messages").with(user("csrf-user"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postGetsGenericRepliesAndReportsMissingAiKey() throws Exception {
        RequestPostProcessor me = user(UUID.randomUUID().toString());
        mvc.perform(put("/api/me/settings").with(me).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fillerCount\": 12, \"voice\": \"korean\", \"fandom\": \"STAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.fillerCount").value(12))
                .andExpect(jsonPath("$.settings.fandom").value("STAY"));

        JsonNode created = body(mvc.perform(post("/api/messages").with(me).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"ate tteokbokki today\",\"image\":\"data:image/png;base64,iVBORw0KGgo=\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("ate tteokbokki today"))
                .andExpect(jsonPath("$.replyCount").value(12)));
        long id = created.path("id").asLong();
        assertThat(created.path("imageUrl").asString()).isEqualTo("/api/messages/" + id + "/image");

        // Generation runs in the background; without an API key it fails quickly.
        JsonNode summary = null;
        for (int i = 0; i < 50; i++) {
            summary = body(mvc.perform(get("/api/messages/" + id).with(me)));
            if (!summary.path("status").asString().equals("pending")) break;
            Thread.sleep(100);
        }
        assertThat(summary.path("status").asString()).isEqualTo("error");
        assertThat(summary.path("error").asString()).contains("OPENROUTER_API_KEY");

        mvc.perform(get("/api/messages/" + id + "/replies").with(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].ai").value(false));
        mvc.perform(get("/api/messages/" + id + "/image").with(me))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mvc.perform(delete("/api/messages/" + id).with(me).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/messages").with(me)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void usersCannotSeeEachOthersMessages() throws Exception {
        RequestPostProcessor alice = user("alice-" + UUID.randomUUID());
        RequestPostProcessor bob = user("bob-" + UUID.randomUUID());
        long id = body(mvc.perform(post("/api/messages").with(alice).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"secret\"}"))).path("id").asLong();

        mvc.perform(get("/api/messages/" + id).with(bob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/messages/" + id + "/replies").with(bob)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/messages/" + id).with(bob).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/messages").with(bob)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsPaidModelsByDefault() throws Exception {
        mvc.perform(put("/api/me/settings").with(user("model-user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"model\":\"openai/gpt-5\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parsesRepliesLeniently() {
        String raw = """
                Sure! Here you go:
                ```json
                [{"name": "@moon_stan", "text": "  eat well!! "}, "omg hi", {"name": "x", "text": ""}]
                ```""";
        var replies = generator.parseReplies(raw);
        assertThat(replies).hasSize(2);
        assertThat(replies.get(0)).isEqualTo(new ReplyGenerator.FanReply("moon_stan", "eat well!!"));
        assertThat(replies.get(1).text()).isEqualTo("omg hi");
        assertThat(replies.get(1).name()).isNotBlank();
    }
}
