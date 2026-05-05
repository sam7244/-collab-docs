package com.collabdocs.document.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceClientTest {

    @Mock RestTemplate restTemplate;
    @Mock CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    @Mock CircuitBreaker circuitBreaker;

    @Test
    void getUserById_success_returnsUserInfo() {
        when(circuitBreakerFactory.create("auth-service")).thenReturn(circuitBreaker);
        AuthServiceClient client = new AuthServiceClient(restTemplate, circuitBreakerFactory);

        UUID userId = UUID.randomUUID();
        AuthServiceClient.UserInfo expectedUser =
                new AuthServiceClient.UserInfo(userId, "user@test.com", "User");

        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(anyString(), eq(AuthServiceClient.UserInfo.class)))
                .thenReturn(expectedUser);

        AuthServiceClient.UserInfo result = client.getUserById(userId);

        assertThat(result).isEqualTo(expectedUser);
    }

    @Test
    void getUserById_circuitBreakerOpen_returnsNull() {
        when(circuitBreakerFactory.create("auth-service")).thenReturn(circuitBreaker);
        AuthServiceClient client = new AuthServiceClient(restTemplate, circuitBreakerFactory);

        UUID userId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Function<Throwable, ?>) inv.getArgument(1))
                        .apply(new RuntimeException("Service unavailable")));

        AuthServiceClient.UserInfo result = client.getUserById(userId);

        assertThat(result).isNull();
    }
}
