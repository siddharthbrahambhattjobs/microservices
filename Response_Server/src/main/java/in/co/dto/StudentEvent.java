package in.co.dto;

import java.util.UUID;

// A Record automatically provides constructors, getters (e.g., student()), equals, hashCode, and toString.
public record StudentEvent(String uUID, String eventType, Student student, String eventId) {
	// Compact constructor to handle default values
	public StudentEvent {
		if (uUID == null) {
			uUID = UUID.randomUUID().toString();
		}
	}

	// Overloaded constructor to match your previous usage (omitting eventId)
	public StudentEvent(String uUID, String eventType, Student student) {
		this(uUID, eventType, student, null);
	}
}