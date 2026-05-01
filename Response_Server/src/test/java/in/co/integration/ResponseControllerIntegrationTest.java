package in.co.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.co.dto.Student;
import in.co.repo.StudentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResponseControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private StudentRepository repository;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private Student saved; // id always DB-generated — never set manually

	private String url(String path) {
		return "http://localhost:" + port + "/response" + path;
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll(); // clean slate — prevents id=0 PK clashes between tests
		Student s = new Student();
		s.setName("Alice");
		s.setStatus("ACTIVE");
		saved = repository.save(s); // H2 assigns the id
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	// -----------------------------------------------------------------------
	// GET /response/getStudents/{id}
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("GET /getStudents/{id} – 200 OK with correct student body")
	void getStudent_found_returns200() throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url("/getStudents/" + saved.getId()))).GET()
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		Student body = objectMapper.readValue(response.body(), Student.class);
		assertThat(body.getName()).isEqualTo("Alice");
		assertThat(body.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	@DisplayName("GET /getStudents/{id} – 404 Not Found for unknown id")
	void getStudent_notFound_returns404() throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url("/getStudents/" + Integer.MAX_VALUE))).GET()
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(404);
	}

	// -----------------------------------------------------------------------
	// GET /response/getAllStudent
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("GET /getAllStudent – 200 OK returns all students")
	void getAllStudent_nonEmpty_returns200() throws Exception {
		Student second = new Student();
		second.setName("Bob");
		second.setStatus("INACTIVE");
		repository.save(second);

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url("/getAllStudent"))).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		List<Student> body = objectMapper.readValue(response.body(), new TypeReference<>() {
		});
		assertThat(body).hasSize(2);
		assertThat(body).extracting(Student::getName).containsExactlyInAnyOrder("Alice", "Bob");
	}
}