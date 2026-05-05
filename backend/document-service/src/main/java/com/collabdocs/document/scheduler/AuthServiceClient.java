package com.collabdocs.document.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;

    @SuppressWarnings("rawtypes")
    public AuthServiceClient(RestTemplate restTemplate, CircuitBreakerFactory circuitBreakerFactory) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreakerFactory.create("auth-service");
    }

    public UserInfo getUserById(UUID userId) {
        return circuitBreaker.run(
            () -> restTemplate.getForObject(
                "http://auth-service/auth/internal/users/" + userId,
                UserInfo.class
            ),
            throwable -> {
                log.warn("Circuit breaker: auth-service unavailable for user {}: {}",
                        userId, throwable.getMessage());
                return null;
            }
        );
    }

    public record UserInfo(UUID id, String email, String displayName) {}
}
