package sk.foundation.techdemo.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	// CORS is configured in SecurityConfig#corsConfigurationSource and registered in the security
	// filter chain (.cors(...)), so preflight is handled before authorization. Do not also define
	// addCorsMappings here — the security-chain CORS is the single source of truth.

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("/static")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {

					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						// Show index.html if no resource was found
						if (!location.createRelative(resourcePath).exists()
								&& !location.createRelative(resourcePath).isReadable()) {
							return location.createRelative("index.html");
						} else {
							return location.createRelative(resourcePath);
						}
					}
				});
	}
}
