package in.co.schedular;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutorConfig {

	@Bean(name = "outboxSchedulerExecutor")
	Executor outboxSchedulerExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2); // 2 threads — enough for outbox relay
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(10);
		executor.setThreadNamePrefix("OutboxRelay-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy()); // skip if overloaded
		executor.initialize();
		return executor;
	}
}
