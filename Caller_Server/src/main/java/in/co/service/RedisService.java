package in.co.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisService {

	private final RedisTemplate<Object, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisService(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public <T> T get(String key, Class<T> entityClass) {
		try {
			var o = redisTemplate.opsForValue().get(key);
			if (o != null) {
				var cleanJson = ((String) o).replaceAll("[\\x00-\\x1F]", "");
				return objectMapper.readValue(cleanJson, entityClass);
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Redis GET failed for key: " + key, e);
		}
	}

	// CHANGED: was Student-specific, now generic <T>
	public <T> void set(String key, T entity, Long ttlSeconds) {
		try {
			var jsonValue = objectMapper.writeValueAsString(entity);
			redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Redis SET failed for key: " + key, e);
		}
	}

	/**
	 * NEW: Atomic Redis SETNX + TTL.
	 *
	 * Returns TRUE → key was newly created → first/unique request, proceed
	 * normally. Returns FALSE → key already existed → duplicate request,
	 * short-circuit.
	 *
	 * Using Duration overload keeps the SET and EXPIRE in a single Redis command,
	 * eliminating the race window that exists between SET + separate EXPIRE calls.
	 */
	public boolean setIfAbsent(String key, Object value, long ttlSeconds) {
		try {
			var jsonValue = objectMapper.writeValueAsString(value);
			Boolean result = redisTemplate.opsForValue().setIfAbsent(key, jsonValue, Duration.ofSeconds(ttlSeconds));
			return Boolean.TRUE.equals(result);
		} catch (Exception e) {
			throw new RuntimeException("Redis SETNX failed for key: " + key, e);
		}
	}

	public void delete(String redisKey) {
		redisTemplate.delete(redisKey);
	}
}