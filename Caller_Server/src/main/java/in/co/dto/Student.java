package in.co.dto;

public record Student(Integer id, String name, String course, String status) {
	// Helper method to create a copy with a new status
	public Student withStatus(String newStatus) {
		return new Student(this.id(), this.name(), this.course(), newStatus);
	}
}