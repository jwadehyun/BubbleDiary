package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the real cookie round trip the front end relies on. It needs its own application context (hence the
 * property) because the csrf() test helper used in other tests swaps the app's CSRF token repository.
 */
@SpringBootTest(properties = "test.context=csrf-cookie")
@AutoConfigureMockMvc
class CsrfCookieTests {

    @Autowired
    MockMvc mvc;

    @Test
    void frontEndCanEchoCsrfCookie() throws Exception {
        RequestPostProcessor me = BubbleDiaryApplicationTests.user("cookie-user");
        var cookie = mvc.perform(get("/api/me").with(me)).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isFalse();
        mvc.perform(post("/api/messages").with(me).cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isOk());
    }
}
