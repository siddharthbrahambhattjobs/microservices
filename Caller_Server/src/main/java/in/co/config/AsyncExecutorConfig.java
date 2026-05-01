package in.co.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncExecutorConfig {

	// Java 21 code
	// from spring 3.2 we don't need to use any code related to thread creation just
	// spring.threads.virtual.enabled=true
	// This single line makes Spring Boot:
	// Run tomcat on virtual threads (every HTTP request gets its own virtual
	// thread)
	// Auto-configure @Async to use virtual threads
	// No AsyncExecutorConfig class needed at all

	/*
	 * @Bean(name = "bulkRequestExecutor") TaskExecutorAdapter bulkRequestExecutor()
	 * { // Java 21: One virtual thread per task. Extremely lightweight and fast.
	 * return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
	 * }
	 */

	// Old versions
	@Bean(name = "bulkRequestExecutors")
	Executor bulkRequestExecutors() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 1. Base number of threads kept alive
		executor.setCorePoolSize(100); // 2.
		// Maximum threads to spawn if the queue is full executor.setMaxPoolSize(500);

		// 3. How many requests can wait in line before we start rejecting or blocking
		executor.setQueueCapacity(2000);
		executor.setThreadNamePrefix("BulkAsync-");

		// 4. CRITICAL: Backpressure mechanism. // If the queue of 20,000 is full, force
		// the Tomcat HTTP thread to execute the // task itself. // This naturally slows
		// down the ingress rate without dropping data.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.initialize();
		return executor;
	}
}
