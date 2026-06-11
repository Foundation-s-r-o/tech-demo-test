package sk.foundation.techdemo.auth;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Simple session-based security for the local demo.
 * <p>
 * NOTE: CSRF is disabled because this is a JSON API consumed by a same-origin SPA in a local
 * demo with no production data. When auth moves to Keycloak (see docs/UPGRADE_PLAN.md), revisit
 * CSRF and session handling.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
		"/api/auth/login",
		"/actuator/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/v3/api-docs/**",
		"/h2-console/**",
	};

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	/**
	 * Permissive CORS for the local SPA dev server (origin :8080 calling the API on :8082).
	 * allowCredentials is required so the session cookie flows; with credentials the spec forbids
	 * "*" for origins, so we use origin patterns. Demo-only — tighten allowed origins for any real
	 * deployment (tracked as a demo-mode caveat in SECURITY.md).
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("*"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Register CORS in the security filter chain so preflight OPTIONS requests are
				// handled (and short-circuited) BEFORE authorization. Without this, a credential-less
				// preflight to an authenticated endpoint (e.g. POST /api/auth/logout) hits
				// .anyRequest().authenticated() and 401s, so the browser blocks the real request.
				// Uses the corsConfigurationSource() bean below.
				.cors(Customizer.withDefaults())
				// SECURITY: CSRF deliberately disabled for the local demo (JSON API + same-origin SPA,
				// no production data). Tracked as a demo-mode caveat in SECURITY.md; must be re-enabled
				// before any real deployment (CSRF token endpoint + header check on state-changing
				// requests). CodeQL alert java/spring-disabled-csrf-protection is dismissed accordingly.
				// lgtm[java/spring-disabled-csrf-protection]
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				// allow the H2 console to render in a frame
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}
}
