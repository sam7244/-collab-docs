package com.collabdocs.auth.service;

import com.collabdocs.auth.dto.AuthResponse;
import com.collabdocs.auth.dto.LoginRequest;
import com.collabdocs.auth.dto.SignupRequest;
import com.collabdocs.auth.entity.User;
import com.collabdocs.auth.exception.ConflictException;
import com.collabdocs.auth.exception.UnauthorizedException;
import com.collabdocs.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.nullable;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void signup_withNewEmail_returnsAuthResponse() {
        SignupRequest req = new SignupRequest("alice@test.com", "password123", "Alice");
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(nullable(UUID.class), eq("alice@test.com"))).thenReturn("token123");

        AuthResponse response = authService.signup(req);

        assertThat(response.token()).isEqualTo("token123");
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.displayName()).isEqualTo("Alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_withDuplicateEmail_throwsConflictException() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("alice@test.com", "pass123", "Alice")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        User user = new User("bob@test.com", "hashedPass", "Bob");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashedPass")).thenReturn(true);
        when(jwtService.generateToken(nullable(UUID.class), eq("bob@test.com"))).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("bob@test.com", "secret"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("bob@test.com");
    }

    @Test
    void login_withWrongPassword_throwsUnauthorizedException() {
        User user = new User("bob@test.com", "hashedPass", "Bob");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashedPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob@test.com", "wrongpass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorizedException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@test.com", "pass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
