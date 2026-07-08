package sk.foundation.techdemo.records.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import sk.foundation.techdemo.BaseIT;
import sk.foundation.techdemo.auditlog.AccessLogRepository;

/**
 * Vital-records API on a dedicated in-memory database with real seeded principals: office scoping
 * (T8 — clerk restricted to own office, supervisor nationwide), registry-number uniqueness,
 * amendment (supervisor + mandatory reason), dissolution, and Art. 9 medical data.
 * Seeded: {@code user} = CLERK @ office 2957, {@code admin} = SUPERVISOR (central).
 */
@Transactional
@WithUserDetails("user")
@TestPropertySource(properties =
		"spring.datasource.url=jdbc:h2:mem:registryrecordsit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class RegistryRecordsApiIT extends BaseIT {

	private static final String OWN_OFFICE = "2957";
	private static final String OTHER_OFFICE = "2958";
	private static final String PURPOSE = "X-Access-Purpose";

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AccessLogRepository accessLogRepository;

	@Test
	void clerkRegistersAndReadsBirthForOwnOffice() throws Exception {
		String child = createPerson();
		String id = registerBirth(OWN_OFFICE, 2024, 10, child);
		mockMvc.perform(get("/api/births/{id}", id).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childUid").value(child))
				.andExpect(jsonPath("$.officeCode").value(OWN_OFFICE));
	}

	@Test
	void clerkCannotRegisterForAnotherOffice() throws Exception {
		String child = createPerson();
		mockMvc.perform(post("/api/births").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OTHER_OFFICE, 2024, 11, child)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithUserDetails("admin")
	void supervisorRegistersForAnyOffice() throws Exception {
		String child = createPerson();
		mockMvc.perform(post("/api/births").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OTHER_OFFICE, 2024, 12, child)))
				.andExpect(status().isCreated());
	}

	@Test
	void duplicateRegistryNumberIsRejected() throws Exception {
		String child = createPerson();
		mockMvc.perform(post("/api/births").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OWN_OFFICE, 2024, 20, child)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/births").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OWN_OFFICE, 2024, 20, child)))
				.andExpect(status().isConflict());
	}

	@Test
	void clerkCannotAmend() throws Exception {
		String id = registerBirth(OWN_OFFICE, 2024, 30, createPerson());
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OWN_OFFICE, 2024, 30, createPerson())))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithUserDetails("admin")
	void supervisorAmendmentRequiresReason() throws Exception {
		String child = createPerson();
		String id = registerBirth(OTHER_OFFICE, 2024, 40, child);
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(OTHER_OFFICE, 2024, 40, child)))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJsonWithReason(OTHER_OFFICE, 2024, 40, child, "corrected time")))
				.andExpect(status().isCreated());
	}

	@Test
	@WithUserDetails("admin")
	void amendingSupersededVersionIsRejected() throws Exception {
		String child = createPerson();
		String id = registerBirth(OTHER_OFFICE, 2024, 60, child);
		// first amendment supersedes the original version
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJsonWithReason(OTHER_OFFICE, 2024, 60, child, "first fix")))
				.andExpect(status().isCreated());
		// amending the now-superseded original must fail (no forked chains)
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJsonWithReason(OTHER_OFFICE, 2024, 60, child, "second fix")))
				.andExpect(status().isConflict());
	}

	@Test
	@WithUserDetails("admin")
	void amendOntoAnotherRecordsRegistryNumberIsRejected() throws Exception {
		String child = createPerson();
		registerBirth(OTHER_OFFICE, 2024, 61, child);            // record A holds entry 61
		String recordB = registerBirth(OTHER_OFFICE, 2024, 62, child); // record B holds entry 62
		mockMvc.perform(post("/api/births/{id}/amendments", recordB).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJsonWithReason(OTHER_OFFICE, 2024, 61, child, "collide with A")))
				.andExpect(status().isConflict());
	}

	@Test
	@WithUserDetails("admin")
	void legitimateAmendKeepingSameNumberSucceeds() throws Exception {
		String child = createPerson();
		String id = registerBirth(OTHER_OFFICE, 2024, 63, child);
		mockMvc.perform(post("/api/births/{id}/amendments", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJsonWithReason(OTHER_OFFICE, 2024, 63, child, "keep same number")))
				.andExpect(status().isCreated());
	}

	@Test
	@WithUserDetails("admin")
	void deathWithoutAnyBasisIsBadRequest() throws Exception {
		String person = createPerson();
		String body = "{\"officeCode\":\"" + OTHER_OFFICE + "\",\"bookYear\":2022,\"entryNo\":2,\"personUid\":\""
				+ person + "\",\"declaredByCourt\":false}";
		mockMvc.perform(post("/api/deaths").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithUserDetails("admin")
	void marriageAndDissolution() throws Exception {
		String spouse1 = createPerson();
		String spouse2 = createPerson();
		String id = registerMarriage(spouse1, spouse2);
		mockMvc.perform(get("/api/marriages/{id}", id).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spouse1Uid").value(spouse1));
		mockMvc.perform(post("/api/marriages/{id}/dissolutions", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dissolutionType\":\"divorce\",\"effectiveDate\":\"2020-01-01\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	@WithUserDetails("admin")
	void marriageReadLogsAccessForBothSpouses() throws Exception {
		String spouse1 = createPerson();
		String spouse2 = createPerson();
		String id = registerMarriage(spouse1, spouse2);
		mockMvc.perform(get("/api/marriages/{id}", id).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		// the read touches both subjects -> a lookup access-log row for each spouse
		assertTrue(accessLogRepository.findByPersonUidOrderByAccessedAtDesc(spouse1).stream()
				.anyMatch(a -> "lookup".equals(a.getPurpose())), "spouse1 read must be access-logged");
		assertTrue(accessLogRepository.findByPersonUidOrderByAccessedAtDesc(spouse2).stream()
				.anyMatch(a -> "lookup".equals(a.getPurpose())), "spouse2 read must be access-logged");
	}

	@Test
	@WithUserDetails("admin")
	void deathAndMedical() throws Exception {
		String person = createPerson();
		String id = registerDeath(person);
		mockMvc.perform(get("/api/deaths/{id}", id).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personUid").value(person));
		mockMvc.perform(post("/api/deaths/{id}/medical", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"causeOfDeathIcd10\":\"I21\",\"autopsyPerformed\":false,\"certifiedBy\":\"Dr. A\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/deaths/{id}/medical", id).header(PURPOSE, "review")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.causeOfDeathIcd10").value("I21"));
	}

	// --- helpers -------------------------------------------------------------

	private String createPerson() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/persons").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"givenNames\":\"Jan\",\"familyName\":\"Novak\",\"sex\":\"M\","
						+ "\"dateOfBirth\":\"1990-01-01\",\"citizenship\":\"703\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("uid").asString();
	}

	private String registerBirth(String office, int year, int entry, String childUid) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/births").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(birthJson(office, year, entry, childUid)))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("recordId").asString();
	}

	private String registerMarriage(String spouse1, String spouse2) throws Exception {
		String body = "{\"officeCode\":\"" + OTHER_OFFICE + "\",\"bookYear\":2015,\"entryNo\":1,\"spouse1Uid\":\""
				+ spouse1 + "\",\"spouse2Uid\":\"" + spouse2 + "\",\"marriageDate\":\"2015-06-01\","
				+ "\"marriageForm\":\"civil\"}";
		MvcResult result = mockMvc.perform(post("/api/marriages").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("recordId").asString();
	}

	private String registerDeath(String personUid) throws Exception {
		String body = "{\"officeCode\":\"" + OTHER_OFFICE + "\",\"bookYear\":2022,\"entryNo\":1,\"personUid\":\""
				+ personUid + "\",\"dateOfDeath\":\"2022-03-03\",\"declaredByCourt\":false}";
		MvcResult result = mockMvc.perform(post("/api/deaths").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("recordId").asString();
	}

	private static String birthJson(String office, int year, int entry, String childUid) {
		return "{\"officeCode\":\"" + office + "\",\"bookYear\":" + year + ",\"entryNo\":" + entry
				+ ",\"childUid\":\"" + childUid + "\",\"dateOfBirth\":\"2024-05-01\"}";
	}

	private static String birthJsonWithReason(String office, int year, int entry, String childUid, String reason) {
		return "{\"officeCode\":\"" + office + "\",\"bookYear\":" + year + ",\"entryNo\":" + entry
				+ ",\"childUid\":\"" + childUid + "\",\"dateOfBirth\":\"2024-05-01\",\"amendmentReason\":\""
				+ reason + "\"}";
	}
}
