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
		registry.addMapping("/**").allowedMethods("*");
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
