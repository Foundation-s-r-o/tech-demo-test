package sk.foundation.techdemo.auth;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Session-based security for the JSON API and SPA.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
		"/api/auth/login",
		"/api/auth/csrf",
		"/actuator/health",
		"/actuator/health/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/v3/api-docs/**",
	};

	// Role names (without the ROLE_ prefix that hasRole/hasAnyRole add automatically).
	private static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_USER = "USER";

	// Defense-in-depth CSP for API-origin responses (error pages, and swagger-ui outside prod).
	// The SPA is served from its own origin and ships its own CSP, so this does not govern it.
	// script-src is 'self' only (no unsafe-eval/inline script — the prod bundle is eval-free,
	// SEC-13); 'unsafe-inline' is permitted for styles only, which swagger-ui and Bootstrap need.
	private static final String CSP_POLICY = String.join("; ",
		"default-src 'self'",
		"script-src 'self'",
		"style-src 'self' 'unsafe-inline'",
		"img-src 'self' data:",
		"font-src 'self' data:",
		"connect-src 'self'",
		"object-src 'none'",
		"base-uri 'self'",
		"form-action 'self'",
		"frame-ancestors 'none'");

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	/**
	 * Credentialed CORS is restricted to explicitly configured SPA origins.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@org.springframework.beans.factory.annotation.Value("${app.security.allowed-origins}")
			List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Cache-Control", "Pragma", "Expires"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(csrfTokenRequestHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						// Authorization matrix (default): ADMIN = full CRUD + PDF export + ops
						// endpoints; USER = read-only (list + detail). See SECURITY.md.
						.requestMatchers(HttpMethod.GET, "/api/persons/*/pdf").hasRole(ROLE_ADMIN)
						.requestMatchers(HttpMethod.GET, "/api/persons", "/api/persons/**")
								.hasAnyRole(ROLE_USER, ROLE_ADMIN)
						.requestMatchers("/api/persons", "/api/persons/**").hasRole(ROLE_ADMIN)
						.requestMatchers("/actuator/**").hasRole(ROLE_ADMIN)
						.anyRequest().authenticated())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
						.referrerPolicy(referrer -> referrer
								.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
						.permissionsPolicyHeader(policy -> policy
								.policy("camera=(), geolocation=(), microphone=()")));
		return http.build();
	}
}
