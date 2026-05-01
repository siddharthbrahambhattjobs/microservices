package in.co.dto;

public record StudentEvent(String uuid, String eventType, Student student, String eventId) {

	/**
	 * Convenience constructor for events where eventId is not needed.
	 */
	public StudentEvent(String uuid, String eventType, Student student) {
		this(uuid, eventType, student, null);
	}
}