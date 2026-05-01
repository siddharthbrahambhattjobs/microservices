package in.co.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import in.co.dto.Student;

@Component
@FeignClient(name = "response-client", url = "http://${RESPONSE_SERVER_SVC:localhost}:8154/response")
public interface CallerClient {

	@GetMapping("/getStudents/{id}")
	ResponseEntity<Student> getStudent(@PathVariable int id);

	@GetMapping("/getAllStudent")
	ResponseEntity<List<Student>> getAllStudent();

	@PostMapping("/create")
	ResponseEntity<Student> createStudent(@RequestBody Student student);
}
