package in.co.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.co.dto.IdempotencyRecord;

@Service
public class CallerIdempotencyService {

	private static final String PREFIX = "idem:req:";
	private RedisTemplate<String, String> redisTemplate;
	private ObjectMapper objectMapper;

	CallerIdempotencyService(RedisTemplate<String, String> template, ObjectMapper mapper) {
		this.redisTemplate = template;
		this.objectMapper = mapper;
	}

	public boolean tryStart(String key, String correlationId, Object request, Duration ttl) {
		try {
			String requestHash = sha256(objectMapper.writeValueAsString(request));
			IdempotencyRecord record = new IdempotencyRecord(key, correlationId, "PROCESSING", requestHash, null);
			String value = objectMapper.writeValueAsString(record);
			Boolean ok = redisTemplate.opsForValue().setIfAbsent(PREFIX + key, value, ttl);
			return Boolean.TRUE.equals(ok);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create idempotency record", e);
		}
	}

	public IdempotencyRecord find(String key) {
		try {
			String json = redisTemplate.opsForValue().get(PREFIX + key);
			return json == null ? null : objectMapper.readValue(json, IdempotencyRecord.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read idempotency record", e);
		}
	}

	public void markCompleted(String key, String correlationId, String responseBody, Duration ttl) {
		try {
			IdempotencyRecord record = new IdempotencyRecord(key, correlationId, "COMPLETED", null, responseBody);
			redisTemplate.opsForValue().set(PREFIX + key, objectMapper.writeValueAsString(record), ttl);
		} catch (Exception e) {
			throw new RuntimeException("Failed to update idempotency record", e);
		}
	}

	private String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}

}