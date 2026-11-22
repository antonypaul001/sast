package com.accenture.sast.tracker;

import com.accenture.sast.tracker.repository.AppUserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "API_SECRET=test-secret")
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;

    @BeforeEach
    void clean() { userRepo.deleteAll(); }

    @Test
    void register_thenLogin_succeeds() throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of("username", "frank", "password", "pass123"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered"));

        String loginBody = mapper.writeValueAsString(Map.of("username", "frank", "password", "pass123"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of("username", "grace", "password", "correct"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());

        String loginBody = mapper.writeValueAsString(Map.of("username", "grace", "password", "wrong"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        String loginBody = mapper.writeValueAsString(Map.of("username", "nobody", "password", "whatever"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
    }
}
