package in.co.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Guards the Kafka consumer against at-least-once redelivery.
 *
 * Uses Redis SETNX so the claim is atomic — two parallel redeliveries of the
 * same correlationId can't both return true.
 *
 * Fail-open: if Redis itself is down we let the message through rather than
 * silently dropping it. The DB unique constraint on correlationId is the final
 * safety net in that case (see schema note below).
 */
@Service
public class IdempotencyService {

	private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
	private static final long TTL_SECONDS = 86_400L; // 24 h — covers any realistic Kafka retry window
	private static final String KEY_PREFIX = "processed:cmd:";

	private final RedisTemplate<Object, Object> redisTemplate;

	public IdempotencyService(RedisTemplate<Object, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * Attempts to claim exclusive processing rights for the given correlationId.
	 *
	 * @return true → first time seen, safe to process. false → already claimed by a
	 *         previous invocation, skip.
	 */
	public boolean claim(String correlationId) {
		try {
			Boolean acquired = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + correlationId, "PROCESSING",
					Duration.ofSeconds(TTL_SECONDS));
			return Boolean.TRUE.equals(acquired);
		} catch (Exception e) {
			log.warn("Redis unavailable for idempotency check on correlationId={}. "
					+ "Failing open — DB constraint is the fallback.", correlationId, e);
			return true; // fail-open
		}
	}

	/**
	 * Call after successful DB write to update the marker from PROCESSING →
	 * COMPLETED. Keeps the key alive so future redeliveries are still blocked.
	 */
	public void markCompleted(String correlationId) {
		redisTemplate.opsForValue().set(KEY_PREFIX + correlationId, "COMPLETED", Duration.ofSeconds(TTL_SECONDS));
	}

	/**
	 * Call on unrecoverable failure so the key is removed and Kafka can retry. Only
	 * call this if you are sure retrying is safe (e.g., DB constraint will prevent
	 * a true duplicate — don't call if you already committed).
	 */
	public void release(String correlationId) {
		redisTemplate.delete(KEY_PREFIX + correlationId);
	}
}