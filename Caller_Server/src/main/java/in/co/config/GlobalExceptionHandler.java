package in.co.config;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(FeignException.NotFound.class)
	public ResponseEntity<Map<String, String>> handleFeignNotFound(FeignException.NotFound ex) {
		log.warn("Downstream service returned 404: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", "Resource not found", "message", "The requested resource does not exist"));
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<Map<String, String>> handleFeignException(FeignException ex) {
		log.error("Downstream service error: status={}", ex.status(), ex);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Service unavailable", "message",
				"Unable to process your request. Please try again later."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
		log.error("Unexpected error: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				Map.of("error", "Internal server error", "message", "Something went wrong. Please contact support."));
	}
}