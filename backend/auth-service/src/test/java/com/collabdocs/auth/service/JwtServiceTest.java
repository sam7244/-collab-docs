package com.collabdocs.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-long-enough-for-hs256-algorithm");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void parseToken_containsCorrectSubjectAndEmail() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@test.com");

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@test.com");
    }

    @Test
    void parseToken_withExpiredToken_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // already expired
        String token = jwtService.generateToken(UUID.randomUUID(), "user@test.com");

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseToken_withTamperedToken_throwsException() {
        String token = jwtService.generateToken(UUID.randomUUID(), "user@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(Exception.class);
    }
}
