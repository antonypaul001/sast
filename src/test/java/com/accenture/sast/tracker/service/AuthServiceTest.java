package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.config.PasswordConfig;
import com.accenture.sast.tracker.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verifyNoMoreInteractions;


@SpringJUnitConfig(classes = {AuthService.class, PasswordConfig.class})
@TestPropertySource("classpath:application-test.properties")
class AuthServiceTest {

    @MockitoBean
    private AppUserRepository mockUserRepository;

    @Autowired
    private AuthService authService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mockUserRepository);
    }

    @Test
    void getApiSecret() {
        assertThat(authService.getApiSecret(), is("test-secret"));
    }
}