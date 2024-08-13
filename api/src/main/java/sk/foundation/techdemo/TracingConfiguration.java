package sk.foundation.techdemo;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class TracingConfiguration implements WebMvcConfigurer {

	private final Logger log = LoggerFactory.getLogger(TracingConfiguration.class);

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {

			@Override
			public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
					ModelAndView modelAndView) {
				log.info("TraceId in interceptor: {}", Span.current().getSpanContext().getTraceId());
				response.addHeader("X-TraceId", Span.current().getSpanContext().getTraceId());
			}

			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
					throws Exception {
				log.info("TraceId in interceptor preHandle: {}", Span.current().getSpanContext().getTraceId());
				response.addHeader("X-TraceId", Span.current().getSpanContext().getTraceId());
				return true;
			}
		});
	}
}