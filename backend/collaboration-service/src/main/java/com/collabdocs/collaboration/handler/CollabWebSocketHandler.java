package com.collabdocs.collaboration.handler;

import com.collabdocs.collaboration.kafka.EditEventProducer;
import com.collabdocs.collaboration.presence.PresenceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CollabWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CollabWebSocketHandler.class);

    // docId -> set of sessions
    private final Map<String, Set<WebSocketSession>> docSessions = new ConcurrentHashMap<>();
    // sessionId -> docId
    private final Map<String, String> sessionDocMap = new ConcurrentHashMap<>();
    // sessionId -> userId
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final EditEventProducer editEventProducer;
    private final PresenceService presenceService;

    public CollabWebSocketHandler(EditEventProducer editEventProducer, PresenceService presenceService) {
        this.editEventProducer = editEventProducer;
        this.presenceService = presenceService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String docId = extractDocId(session);
        String token = extractQueryParam(session, "token");

        if (docId == null || token == null) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }

        String userId;
        try {
            userId = validateToken(token);
        } catch (JwtException e) {
            log.warn("WebSocket rejected: invalid JWT for docId={}", docId);
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        session.getAttributes().put("docId", docId);
        session.getAttributes().put("userId", userId);

        docSessions.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionDocMap.put(session.getId(), docId);
        sessionUserMap.put(session.getId(), userId);

        presenceService.addUser(docId, userId);
        log.info("User {} connected to doc {} (session {})", userId, docId, session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String docId = sessionDocMap.get(session.getId());
        String userId = sessionUserMap.get(session.getId());
        if (docId == null) return;

        // Broadcast binary Yjs update to all other sessions on same doc
        // Copy payload bytes once — ByteBuffer position advances on each read,
        // so we can't reuse the same BinaryMessage across multiple sends.
        byte[] payload = new byte[message.getPayloadLength()];
        message.getPayload().duplicate().get(payload);

        Set<WebSocketSession> sessions = docSessions.getOrDefault(docId, Collections.emptySet());
        for (WebSocketSession other : sessions) {
            if (!other.getId().equals(session.getId()) && other.isOpen()) {
                try {
                    other.sendMessage(new BinaryMessage(payload));
                } catch (IOException e) {
                    log.warn("Failed to broadcast to session {}: {}", other.getId(), e.getMessage());
                }
            }
        }

        // Publish metadata event to Kafka (not the binary payload)
        editEventProducer.publishEditEvent(docId, userId, message.getPayloadLength());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Echo text messages back for debugging
        try {
            session.sendMessage(message);
        } catch (IOException e) {
            log.warn("Failed to echo text message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String docId = sessionDocMap.remove(session.getId());
        String userId = sessionUserMap.remove(session.getId());
        if (docId != null) {
            Set<WebSocketSession> sessions = docSessions.get(docId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    docSessions.remove(docId);
                }
            }
            if (userId != null) {
                presenceService.removeUser(docId, userId);
            }
            log.info("User {} disconnected from doc {} (session {})", userId, docId, session.getId());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String extractDocId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        // Path: /ws/documents/{docId}
        String prefix = "/ws/documents/";
        if (path != null && path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return null;
    }

    private String extractQueryParam(WebSocketSession session, String param) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                return kv[1];
            }
        }
        return null;
    }

    private String validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try { session.close(status); } catch (IOException ignored) {}
    }
}
