package sk.foundation.techdemo.persons.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PersonController.class)
@WithMockUser
public class PersonControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	PersonApiService personApiService;

	@MockitoBean
	PersonPdfService personPdfService;

	@Test
	void najdiOsobuVrati400AkRequestNieJeSpravny() throws Exception {
		mockMvc.perform(get("/api/persons")
				.param("name", "test"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void getPdfVratiPdfSpravnehoTypu() throws Exception {
		when(personPdfService.generatePersonPdf(eq(1L))).thenReturn("%PDF-1.4 stub".getBytes());

		mockMvc.perform(get("/api/persons/{id}/pdf", 1L))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));
	}
}
