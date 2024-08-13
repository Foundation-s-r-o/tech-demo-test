package sk.foundation.techdemo.persons.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PersonController.class)
public class PersonControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	PersonApiService personApiService;

	@Test
	void najdiOsobuVrati400AkRequestNieJeSpravny() throws Exception {
		mockMvc.perform(get("/api/persons")
				.param("name", "test"))
				.andExpect(status().is4xxClientError());
	}
}
