package com.collabdocs.collaboration.presence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private static final String PRESENCE_KEY_PREFIX = "presence:doc:";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(300);

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addUser(String docId, String userId) {
        String key = PRESENCE_KEY_PREFIX + docId;
        try {
            redisTemplate.opsForSet().add(key, userId);
            redisTemplate.expire(key, PRESENCE_TTL);
        } catch (Exception e) {
            log.warn("Failed to add user to presence: {}", e.getMessage());
        }
    }

    public void removeUser(String docId, String userId) {
        String key = PRESENCE_KEY_PREFIX + docId;
        try {
            redisTemplate.opsForSet().remove(key, userId);
        } catch (Exception e) {
            log.warn("Failed to remove user from presence: {}", e.getMessage());
        }
    }

    public Set<String> getPresence(String docId) {
        String key = PRESENCE_KEY_PREFIX + docId;
        try {
            Set<String> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            log.warn("Failed to get presence: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}
