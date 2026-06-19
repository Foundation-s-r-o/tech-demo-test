package sk.foundation.techdemo.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import sk.foundation.techdemo.BaseIT;

/**
 * Exercises the real login flow against the local integration-test {@code admin/admin} user.
 */
class AuthControllerIT extends BaseIT {

	@Test
	void loginWithLocalTestAdminSucceeds() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.firstName").value("Admin"))
				.andExpect(jsonPath("$.lastName").value("Admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	void csrfEndpointIssuesSpaCookieAndHeaderName() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"nope\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithoutSessionReturns401() throws Exception {
		mockMvc.perform(get("/api/persons").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithoutCsrfTokenReturns403() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginRotatesExistingSessionId() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String originalSessionId = session.getId();

		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(originalSessionId);
	}

	@Test
	void onlyHealthActuatorEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/loggers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void securityResponseHeadersIncludeCsp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Security-Policy"))
				.andExpect(header().string("Content-Security-Policy",
						org.hamcrest.Matchers.containsString("script-src 'self'")));
	}

	@Test
	void corsAllowsOnlyConfiguredLocalOrigin() throws Exception {
		mockMvc.perform(options("/api/persons")
				.header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));

		mockMvc.perform(options("/api/persons")
				.header("Origin", "https://evil.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
