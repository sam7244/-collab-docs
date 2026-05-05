package com.collabdocs.collaboration.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditEventProducerTest {

    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @InjectMocks EditEventProducer producer;

    @Test
    void publishEditEvent_sendsToEditEventsTopic() {
        producer.publishEditEvent("doc-123", "user-456", 100);

        verify(kafkaTemplate, atLeastOnce())
                .send(eq("edit-events"), eq("doc-123"), anyString());
    }

    @Test
    void publishEditEvent_sendsToAuditLogTopic() {
        producer.publishEditEvent("doc-123", "user-456", 100);

        verify(kafkaTemplate, atLeastOnce())
                .send(eq("audit-log"), eq("doc-123"), anyString());
    }

    @Test
    void publishEditEvent_payloadContainsDocIdAndUserId() {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        producer.publishEditEvent("doc-123", "user-456", 42);

        verify(kafkaTemplate, times(2))
                .send(anyString(), eq("doc-123"), payloadCaptor.capture());

        for (String payload : payloadCaptor.getAllValues()) {
            assertThat(payload).contains("doc-123");
            assertThat(payload).contains("user-456");
        }
    }

    @Test
    void publishEditEvent_whenKafkaThrows_doesNotPropagate() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        producer.publishEditEvent("doc-123", "user-456", 10);  // should not throw
    }
}
