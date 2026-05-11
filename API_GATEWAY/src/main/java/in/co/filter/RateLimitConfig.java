// src/main/java/in/co/filter/RateLimitConfig.java
package in.co.filter;

import org.springframework.stereotype.Component;

@Component
public class RateLimitConfig {

	// Industry-standard tiers
	public enum RateLimitTier {
		// maxRequests, windowSeconds
		WRITE(20, 60), // POST/PUT/DELETE — strictest
		READ(100, 60), // GET endpoints
		GLOBAL(200, 60); // fallback catch-all

		private final long maxRequests;
		private final long windowSeconds;

		RateLimitTier(long maxRequests, long windowSeconds) {
			this.maxRequests = maxRequests;
			this.windowSeconds = windowSeconds;
		}

		public long maxRequests() {
			return maxRequests;
		}

		public long windowSeconds() {
			return windowSeconds;
		}
	}

	public RateLimitTier resolveTier(String method, String path) {
		// Write operations — tightest limit
		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
			return RateLimitTier.WRITE;
		}
		// Read operations
		if ("GET".equalsIgnoreCase(method)) {
			return RateLimitTier.READ;
		}
		return RateLimitTier.GLOBAL;
	}
}