package com.accenture.sast.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("script-src 'self';")
                )
            );
            // .authorizeHttpRequests(auth -> auth
            //     .anyRequest().authenticated()
            // );
        return http.build();
    }
}
