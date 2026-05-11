package in.co.dto;

public record StudentEvent(String correlationId, String eventType, Student student, String eventId) {

	/**
	 * Convenience constructor for events where eventId is not needed.
	 */
	public StudentEvent(String correlationId, String eventType, Student student) {
		this(correlationId, eventType, student, null);
	}
}