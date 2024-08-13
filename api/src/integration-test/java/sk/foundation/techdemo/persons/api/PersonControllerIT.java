package sk.foundation.techdemo.persons.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.foundation.techdemo.BaseIT;

class PersonControllerIT extends BaseIT {
	
	private static final String API_PERSONS = "/api/persons";
	private static final String API_PERSONS_ID = "/api/persons/{id}";
	private static final String ADMIN = "Admin";
	private static final String ADMIN_EMAIL = "admin@tech-demo.sk";

	@Autowired
	ObjectMapper objectMapper;

	@Test
	@Disabled("cache impl gives different results")
	@Sql({ "/sql/clearAll.sql" })
	void getPersonDetail_notFound() throws Exception {
		mockMvc.perform(get(API_PERSONS_ID, 1).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void getPersonDetail_success() throws Exception {
		mockMvc.perform(get(API_PERSONS_ID, 1).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value((1L)))
				.andExpect(jsonPath("$.firstName").value(ADMIN))
				.andExpect(jsonPath("$.lastName").value(ADMIN))
				.andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
	}

	@Test
	@Disabled("cache impl gives different results")
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void getPersonList_success() throws Exception {
		mockMvc.perform(
				get(API_PERSONS).accept(MediaType.APPLICATION_JSON)
						.param("sortBy", "NAME")
						.param("sortDesc", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value((2L)))
				.andExpect(jsonPath("$.elements[0].id").value((1L)))
				.andExpect(jsonPath("$.elements[0].firstName").value(ADMIN))
				.andExpect(jsonPath("$.elements[0].lastName").value(ADMIN))
				.andExpect(jsonPath("$.elements[0].email").value(ADMIN_EMAIL));
	}

	@Test
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void createPerson_success() throws Exception {
		PersonModifyRequestDTO dto = new PersonModifyRequestDTO();
		dto.setFirstName("fn");
		dto.setLastName("ln");
		dto.setEmail("fn@ln.com");
		String content = objectMapper.writer().writeValueAsString(dto);
		mockMvc.perform(
				post(API_PERSONS).accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(status().isCreated());
	}

	@Test
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void createPerson_conflictingEmail() throws Exception {
		PersonModifyRequestDTO dto = new PersonModifyRequestDTO();
		dto.setFirstName("fn");
		dto.setLastName("ln");
		dto.setEmail(ADMIN_EMAIL);
		String content = objectMapper.writer().writeValueAsString(dto);
		mockMvc.perform(
				post(API_PERSONS).accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(status().is(409));
	}

	@Test
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void updatePerson_success() throws Exception {
		PersonModifyRequestDTO dto = new PersonModifyRequestDTO();
		dto.setFirstName("fn#new");
		dto.setLastName("ln#new");
		dto.setEmail("fn@ln.com");
		String content = objectMapper.writer().writeValueAsString(dto);
		mockMvc.perform(
				put(API_PERSONS_ID, 1).accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(status().isOk());
	}

	@Test
	@Sql({ "/sql/clearAll.sql", "/sql/data.sql" })	
	void updatePerson_conflictingEmail() throws Exception {
		PersonModifyRequestDTO dto = new PersonModifyRequestDTO();
		dto.setFirstName("fn#new");
		dto.setLastName("ln#new");
		dto.setEmail(ADMIN_EMAIL);
		String content = objectMapper.writer().writeValueAsString(dto);
		mockMvc.perform(
				put(API_PERSONS_ID, 2).accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(status().is(409));
	}

}
