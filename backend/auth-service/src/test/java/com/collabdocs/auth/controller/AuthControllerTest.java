package com.collabdocs.auth.controller;

import com.collabdocs.auth.dto.AuthResponse;
import com.collabdocs.auth.dto.UserResponse;
import com.collabdocs.auth.entity.User;
import com.collabdocs.auth.exception.ConflictException;
import com.collabdocs.auth.exception.UnauthorizedException;
import com.collabdocs.auth.repository.UserRepository;
import com.collabdocs.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
    }
)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean UserRepository userRepository;

    @Test
    void signup_withValidBody_returns201AndToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.signup(any()))
                .thenReturn(new AuthResponse("jwt-token", userId, "alice@test.com", "Alice"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "alice@test.com",
                                "password", "password123",
                                "displayName", "Alice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    void signup_withMissingEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123",
                                "displayName", "Alice"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "alice@test.com",
                                "password", "123",
                                "displayName", "Alice"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withDuplicateEmail_returns409() throws Exception {
        when(authService.signup(any())).thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "alice@test.com",
                                "password", "password123",
                                "displayName", "Alice"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.login(any()))
                .thenReturn(new AuthResponse("jwt-token", userId, "bob@test.com", "Bob"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "bob@test.com",
                                "password", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.displayName").value("Bob"));
    }

    @Test
    void login_withBadCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "bob@test.com",
                                "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void me_withValidUserId_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User("alice@test.com", "hashed", "Alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/auth/me")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void me_withUnknownUserId_returns401() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/me")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
