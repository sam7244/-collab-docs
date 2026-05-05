package com.collabdocs.auth.controller;

import com.collabdocs.auth.dto.AuthResponse;
import com.collabdocs.auth.dto.LoginRequest;
import com.collabdocs.auth.dto.SignupRequest;
import com.collabdocs.auth.dto.UserResponse;
import com.collabdocs.auth.entity.User;
import com.collabdocs.auth.repository.UserRepository;
import com.collabdocs.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth", description = "Signup, login, and user info endpoints")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Get current user info (requires X-User-Id header)")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new com.collabdocs.auth.exception.UnauthorizedException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // Internal endpoint — called directly by other services via Eureka, never exposed through Gateway
    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable("userId") UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
