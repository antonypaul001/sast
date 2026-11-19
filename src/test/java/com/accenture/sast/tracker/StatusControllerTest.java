package com.accenture.sast.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "API_SECRET=test-secret")
@AutoConfigureMockMvc
class StatusControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void status_defaultMessage_returnsHtml() throws Exception {
        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Welcome")));
    }

    @Test
    void status_customMessage_reflectedInResponse() throws Exception {
        mvc.perform(get("/api/status").param("message", "Hello World"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello World")));
    }
}
