package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole posting flow with a fake AI, so it runs offline and costs nothing. */
@SpringBootTest
@AutoConfigureMockMvc
class AiRepliesTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    UserRepository users;

    @MockitoBean
    OpenRouterClient openRouter;

    @BeforeEach
    void fakeModel() {
        // More replies than any plan allows, so the server's limits are what decide the counts.
        List<Map<String, String>> replies = new ArrayList<>();
        for (int i = 0; i < 40; i++) replies.add(Map.of("name", "fan" + i, "text", "so proud of you", "kind", "fan"));
        for (int i = 0; i < 3; i++) replies.add(Map.of("name", "anti" + i, "text", "who asked", "kind", "anti"));
        when(openRouter.isConfigured()).thenReturn(true);
        when(openRouter.chat(any(), anyString(), any())).thenReturn(json.writeValueAsString(replies));
    }

    @Test
    void freePostEndsWithOneHundredReplies() throws Exception {
        List<JsonNode> replies = postAndWait(user(UUID.randomUUID().toString()), null);
        assertThat(replies).hasSize(100);
        assertThat(count(replies, false, "fan")).isEqualTo(80);
        assertThat(count(replies, false, "anti")).isEqualTo(4);
        assertThat(count(replies, true, "fan")).isEqualTo(15);
        assertThat(count(replies, true, "anti")).isEqualTo(1);
    }

    @Test
    void premiumPostHasMoreRepliesAndNoAntis() throws Exception {
        String sub = UUID.randomUUID().toString();
        List<JsonNode> replies = postAndWait(user(sub), sub);
        assertThat(replies).hasSize(230);
        assertThat(count(replies, true, "fan")).isEqualTo(30);
        assertThat(replies).noneMatch(r -> r.path("kind").asString().equals("anti"));
    }

    /** Posts a message, waits for the background AI replies, and returns every reply. */
    private List<JsonNode> postAndWait(RequestPostProcessor me, String premiumSub) throws Exception {
        mvc.perform(get("/api/me").with(me)).andExpect(status().isOk());
        if (premiumSub != null) {
            User u = users.findAll().stream().filter(x -> x.getProviderId().equals(premiumSub)).findFirst().orElseThrow();
            u.setPlan(Plan.PREMIUM);
            users.save(u);
        }
        long id = body(mvc.perform(post("/api/messages").with(me).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"first day of tour!\"}"))).path("id").asLong();

        JsonNode summary = null;
        for (int i = 0; i < 50; i++) {
            summary = body(mvc.perform(get("/api/messages/" + id).with(me)));
            if (!summary.path("status").asString().equals("pending")) break;
            Thread.sleep(100);
        }
        assertThat(summary.path("status").asString()).isEqualTo("done");

        List<JsonNode> replies = new ArrayList<>();
        body(mvc.perform(get("/api/messages/" + id + "/replies").with(me))).forEach(replies::add);
        return replies;
    }

    private static long count(List<JsonNode> replies, boolean ai, String kind) {
        return replies.stream()
                .filter(r -> r.path("ai").asBoolean() == ai && r.path("kind").asString().equals(kind))
                .count();
    }

    private static RequestPostProcessor user(String sub) {
        return BubbleDiaryApplicationTests.user(sub);
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
