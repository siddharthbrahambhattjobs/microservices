package in.co.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Redis GET failed for key " + key, e);
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            throw new RuntimeException("Redis SET failed for key " + key, e);
        }
    }

    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, objectMapper.writeValueAsString(value), ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            throw new RuntimeException("Redis SETNX failed for key " + key, e);
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}