// in.co.exception.StudentNotFoundException.java
package in.co.exception;

public class StudentNotFoundException extends RuntimeException {
	private final int studentId;

	public StudentNotFoundException(int studentId) {
		super("Student with ID " + studentId + " does not exist");
		this.studentId = studentId;
	}

	public int getStudentId() {
		return studentId;
	}
}