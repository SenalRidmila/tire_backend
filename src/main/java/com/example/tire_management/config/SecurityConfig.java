package com.example.tire_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // ✅ Disable CSRF for REST API
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll() // Allow all requests without authentication
                )
                .sessionManagement(session -> session.disable()) // ✅ Disable session management for stateless API
                .httpBasic(basic -> basic.disable()) // ✅ Disable HTTP Basic Authentication
                .formLogin(form -> form.disable()) // ✅ Disable form login
                .logout(logout -> logout.disable()) // ✅ Disable logout endpoints
                .build();
    }
}
