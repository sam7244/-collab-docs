package com.collabdocs.collaboration.presence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock SetOperations<String, String> setOps;
    @InjectMocks PresenceService presenceService;

    @Test
    void addUser_callsSaddAndExpire() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        presenceService.addUser("doc-1", "user-1");

        verify(setOps).add("presence:doc:doc-1", "user-1");
        verify(redisTemplate).expire(eq("presence:doc:doc-1"), any(Duration.class));
    }

    @Test
    void addUser_whenRedisThrows_doesNotPropagateException() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        doThrow(new RuntimeException("Redis down")).when(setOps).add(any(), any());

        presenceService.addUser("doc-1", "user-1");  // should not throw
    }

    @Test
    void removeUser_callsSrem() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        presenceService.removeUser("doc-1", "user-1");

        verify(setOps).remove("presence:doc:doc-1", "user-1");
    }

    @Test
    void removeUser_whenRedisThrows_doesNotPropagateException() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        doThrow(new RuntimeException("Redis down")).when(setOps).remove(any(), any());

        presenceService.removeUser("doc-1", "user-1");  // should not throw
    }

    @Test
    void getPresence_returnsSetOfUsers() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members("presence:doc:doc-1")).thenReturn(Set.of("user-1", "user-2"));

        Set<String> result = presenceService.getPresence("doc-1");

        assertThat(result).containsExactlyInAnyOrder("user-1", "user-2");
    }

    @Test
    void getPresence_whenRedisReturnsNull_returnsEmptySet() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(any())).thenReturn(null);

        Set<String> result = presenceService.getPresence("doc-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getPresence_whenRedisThrows_returnsEmptySet() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(any())).thenThrow(new RuntimeException("Redis down"));

        Set<String> result = presenceService.getPresence("doc-1");

        assertThat(result).isEmpty();
    }
}
