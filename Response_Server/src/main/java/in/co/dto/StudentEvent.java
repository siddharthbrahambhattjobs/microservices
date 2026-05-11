package in.co.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // ✅ tolerates extra fields during evolution
public record StudentEvent(String correlationId, String eventType, Student student, String eventId) {

	// ✅ Convenience constructor — eventId optional (Caller uses this)
	public StudentEvent(String correlationId, String eventType, Student student) {
		this(correlationId, eventType, student, null);
	}

	// ✅ Jackson needs this for deserialization of records
	// No-arg constructor not needed — Jackson handles records natively in Java 16+
}
