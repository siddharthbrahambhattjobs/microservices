package in.co.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.co.filter.RateLimitConfig.RateLimitTier;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final StringRedisTemplate redisTemplate;
	private final RateLimitConfig rateLimitConfig;

	public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitConfig rateLimitConfig) {
		this.redisTemplate = redisTemplate;
		this.rateLimitConfig = rateLimitConfig;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String clientIp = extractClientIp(request);
		String path = request.getRequestURI();
		String method = request.getMethod();

		// ✅ Skip rate limiting for health checks, OAuth flow, and CORS preflight
		if (path.startsWith("/actuator")
				|| path.startsWith("/oauth2")
				|| path.startsWith("/login")
				|| "OPTIONS".equalsIgnoreCase(method)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Resolve limit tier: POST /caller/create is stricter than GET endpoints
		RateLimitTier tier = rateLimitConfig.resolveTier(method, path);

		String redisKey = "ratelimit:" + clientIp + ":" + tier.name();
		long windowSeconds = tier.windowSeconds();
		long maxRequests = tier.maxRequests();

		Long count = redisTemplate.opsForValue().increment(redisKey);

		if (count == 1) {
			// First request in window — set expiry
			redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
		}

		// Add standard rate limit headers (industry standard — RFC 6585)
		long ttl = Optional.ofNullable(redisTemplate.getExpire(redisKey, TimeUnit.SECONDS)).orElse(windowSeconds);

		response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - count)));
		response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + ttl));
		response.setHeader("X-RateLimit-Policy", tier.name() + ";w=" + windowSeconds);

		if (count > maxRequests) {
			response.setHeader("Retry-After", String.valueOf(ttl));
			log.warn("Rate limit exceeded. ip={}, path={}, tier={}, count={}/{}", clientIp, path, tier.name(), count,
					maxRequests);

			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("""
					{
					  "status": 429,
					  "error": "Too Many Requests",
					  "message": "Rate limit exceeded. Please slow down.",
					  "retryAfterSeconds": %d
					}
					""".formatted(ttl));
			return;
		}

		filterChain.doFilter(request, response);
	}

	// Handles X-Forwarded-For for requests behind load balancer / K8s ingress
	private String extractClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim(); // first IP is the real client
		}
		return request.getRemoteAddr();
	}
}