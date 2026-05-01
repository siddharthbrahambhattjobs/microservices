package in.co.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class GatewayHttpClientConfig {

	@Bean
	// ✅ RestClientCustomizer is what Spring Cloud Gateway WebMVC
	// actually uses to configure its internal RestClientProxyExchange
	RestClientCustomizer gatewayProxyClientCustomizer() {
		return restClientBuilder -> {

			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
			connectionManager.setMaxTotal(100);
			connectionManager.setDefaultMaxPerRoute(20);

			var httpClient = HttpClients.custom().setConnectionManager(connectionManager)
					.setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(30))
							.setConnectionRequestTimeout(Timeout.ofSeconds(10)).build())
					// ✅ This is the actual fix for chunked encoding
					.disableAutomaticRetries().build();

			restClientBuilder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
		};
	}
}