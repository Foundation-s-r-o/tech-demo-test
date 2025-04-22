package sk.foundation.techdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAIRestTemplateConfig {

	@Value("${openai.api.key}")
	private String openaiApiKey;

	@Bean
	public RestTemplate openaiRestTemplate(RestTemplateBuilder restTemplateBuilder) {
		// Create a RestTemplate with custom timeouts
		// Note: The deprecated methods are still used but should be updated when a solution is found
		// See: https://github.com/spring-projects/spring-boot/issues/42393
		RestTemplate restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(100))
				.build();
		// Add authorization header with API key
		restTemplate.getInterceptors().add((request, body, execution) -> {
			request.getHeaders().add("Authorization", "Bearer " + openaiApiKey);
			return execution.execute(request, body);
		});
		return restTemplate;
	}
}