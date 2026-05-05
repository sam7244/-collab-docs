package com.collabdocs.document.scheduler;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;
    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@collabdocs.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void sendInactivityEmail_sendsMessageSuccessfully() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendInactivityEmail(
                "user@test.com", "User", "My Doc", "doc-id-123", Instant.now());

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInactivityEmail_whenMailSenderThrows_doesNotPropagateException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // Should not throw — EmailService swallows and logs errors
        emailService.sendInactivityEmail(
                "user@test.com", "User", "My Doc", "doc-id-123", Instant.now());
    }
}
