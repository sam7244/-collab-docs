package com.collabdocs.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = FallbackController.class)
class FallbackControllerTest {

    @Autowired WebTestClient webTestClient;

    @Test
    void fallback_returnsServiceUnavailableWithMessage() {
        webTestClient.get().uri("/fallback/auth-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("auth-service is currently unavailable. Please try again later.")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void fallback_anyServiceName_reflects503() {
        webTestClient.get().uri("/fallback/document-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("document-service is currently unavailable. Please try again later.");
    }
}
