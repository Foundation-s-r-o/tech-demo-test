package sk.foundation.techdemo.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import sk.foundation.techdemo.BaseIT;

/**
 * Exercises the real login flow against the Flyway-seeded {@code admin/admin} user — validates
 * the security config, the BCrypt hash in V3, and the session/401 behavior end-to-end.
 */
class AuthControllerIT extends BaseIT {

	@Test
	void loginWithSeededAdminSucceeds() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.firstName").value("Admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"nope\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithoutSessionReturns401() throws Exception {
		mockMvc.perform(get("/api/persons").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
}
