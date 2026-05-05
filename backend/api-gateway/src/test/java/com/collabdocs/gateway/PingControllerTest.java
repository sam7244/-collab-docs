package com.collabdocs.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = PingController.class)
class PingControllerTest {

    @Autowired WebTestClient webTestClient;

    @Test
    void ping_returns200WithPong() {
        webTestClient.get().uri("/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("pong");
    }
}
