package sk.foundation.techdemo.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// Permissive CORS for the local SPA dev server. allowCredentials is required so the
		// session cookie flows; with credentials we must use origin patterns, not "*".
		// Demo-only — tighten the allowed origins for any real deployment.
		registry.addMapping("/**")
				.allowedOriginPatterns("*")
				.allowedMethods("*")
				.allowCredentials(true);
	}

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
