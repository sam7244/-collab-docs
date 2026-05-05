package com.collabdocs.collaboration.handler;

import com.collabdocs.collaboration.kafka.EditEventProducer;
import com.collabdocs.collaboration.presence.PresenceService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollabWebSocketHandlerTest {

    private static final String JWT_SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm";

    @Mock EditEventProducer editEventProducer;
    @Mock PresenceService presenceService;
    @InjectMocks CollabWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "jwtSecret", JWT_SECRET);
    }

    private String validJwt(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private WebSocketSession mockSession(String sessionId, String docId, String token) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        String uriStr = "ws://localhost/ws/documents/" + docId + (token != null ? "?token=" + token : "");
        when(session.getUri()).thenReturn(new URI(uriStr));
        return session;
    }

    @Test
    void afterConnectionEstablished_validJwt_addsSessionAndPresence() throws Exception {
        String userId = "user-1";
        WebSocketSession session = mockSession("session-1", "doc-1", validJwt(userId));

        handler.afterConnectionEstablished(session);

        verify(presenceService).addUser("doc-1", userId);
    }

    @Test
    void afterConnectionEstablished_invalidJwt_closesSession() throws Exception {
        WebSocketSession session = mockSession("session-1", "doc-1", "invalid-token");

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
        verify(presenceService, never()).addUser(any(), any());
    }

    @Test
    void afterConnectionEstablished_missingToken_closesSession() throws Exception {
        WebSocketSession session = mockSession("session-1", "doc-1", null);

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
    }

    @Test
    void handleBinaryMessage_broadcastsToOtherSessionsInSameDoc() throws Exception {
        String userId1 = "user-1";
        String userId2 = "user-2";

        WebSocketSession session1 = mockSession("s1", "doc-1", validJwt(userId1));
        WebSocketSession session2 = mockSession("s2", "doc-1", validJwt(userId2));
        when(session2.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        byte[] payload = "yjs-binary-update".getBytes();
        BinaryMessage message = new BinaryMessage(payload);
        handler.handleBinaryMessage(session1, message);

        verify(session2).sendMessage(any(BinaryMessage.class));
        verify(session1, never()).sendMessage(any(BinaryMessage.class));
        verify(editEventProducer).publishEditEvent(eq("doc-1"), eq(userId1), anyInt());
    }

    @Test
    void handleBinaryMessage_doesNotBroadcastToClosedSession() throws Exception {
        WebSocketSession session1 = mockSession("s1", "doc-1", validJwt("user-1"));
        WebSocketSession session2 = mockSession("s2", "doc-1", validJwt("user-2"));
        when(session2.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.handleBinaryMessage(session1, new BinaryMessage("data".getBytes()));

        verify(session2, never()).sendMessage(any());
    }

    @Test
    void afterConnectionClosed_removesSessionAndPresence() throws Exception {
        String userId = "user-1";
        WebSocketSession session = mockSession("s1", "doc-1", validJwt(userId));
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(presenceService).removeUser("doc-1", userId);
    }

    @Test
    void handleTextMessage_echoesMessageBack() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        TextMessage message = new TextMessage("ping");

        handler.handleTextMessage(session, message);

        verify(session).sendMessage(message);
    }

    @Test
    void handleTextMessage_whenSendThrows_doesNotPropagate() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        doThrow(new IOException("Send failed")).when(session).sendMessage(any());

        handler.handleTextMessage(session, new TextMessage("ping"));
        // no exception expected
    }
}
