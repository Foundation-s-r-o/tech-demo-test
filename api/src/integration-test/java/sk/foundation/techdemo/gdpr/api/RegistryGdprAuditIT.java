package sk.foundation.techdemo.gdpr.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import sk.foundation.techdemo.registry.Person;
import sk.foundation.techdemo.registry.PersonDetail;
import sk.foundation.techdemo.registry.PersonDetailRepository;
import sk.foundation.techdemo.registry.PersonReligion;
import sk.foundation.techdemo.registry.PersonReligionRepository;
import sk.foundation.techdemo.registry.PersonUidService;
import sk.foundation.techdemo.registry.RegistryPersonRepository;

/**
 * Codelists, GDPR workflow, and audit read API on a dedicated in-memory database. Persons are
 * persisted directly (created_by = seeded {@code admin}) so a single principal per test can drive
 * the GDPR/audit endpoints. Seeded principals: dpo, auditor, user (clerk).
 */
@Transactional
@TestPropertySource(properties =
		"spring.datasource.url=jdbc:h2:mem:registrygdprit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class RegistryGdprAuditIT extends BaseIT {

	private static final String CONSENT_BASIS = "art9_2a_consent";

	@Autowired
	private RegistryPersonRepository personRepository;
	@Autowired
	private PersonDetailRepository detailRepository;
	@Autowired
	private PersonReligionRepository religionRepository;
	@Autowired
	private PersonUidService personUidService;
	@Autowired
	private AccessLogRepository accessLogRepository;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@WithUserDetails("user")
	void codelistsExposeItems() throws Exception {
		mockMvc.perform(get("/api/codelists").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/api/codelists/{c}/items", "sex").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
		// newly whitelisted codelists resolve (200 with an array, possibly empty)
		mockMvc.perform(get("/api/codelists/{c}/items", "region").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/api/codelists/{c}/items", "academic_title").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		// a name outside the whitelist is still rejected
		mockMvc.perform(get("/api/codelists/{c}/items", "not_a_codelist").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithUserDetails("dpo")
	void dsrLifecycle() throws Exception {
		String uid = persistPerson();
		Long id = createDsr(uid);
		mockMvc.perform(get("/api/gdpr/requests").param("personUid", uid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("received"))
				.andExpect(jsonPath("$[0].dueAt").isNotEmpty());
		mockMvc.perform(put("/api/gdpr/requests/{id}", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"fulfilled\",\"resolution\":\"provided\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("fulfilled"));
	}

	@Test
	@WithUserDetails("user")
	void clerkCannotFileDsr() throws Exception {
		mockMvc.perform(post("/api/gdpr/requests").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterRef\":\"REQ-1\",\"requestType\":\"access\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithUserDetails("dpo")
	void consentWithdrawalSupersedesReligion() throws Exception {
		String uid = persistPerson();
		Long consentId = createConsent(uid);
		persistReligion(uid, consentId);

		mockMvc.perform(post("/api/gdpr/consents/{id}/withdrawal", consentId).with(csrf()))
				.andExpect(status().isOk());

		// the religion row backed by the withdrawn consent is no longer current
		org.junit.jupiter.api.Assertions.assertTrue(
				religionRepository.findByConsentIdAndSupersededAtIsNull(consentId).isEmpty());
	}

	@Test
	@WithUserDetails("dpo")
	void disclosureAndExportRecordsDisclosure() throws Exception {
		String uid = persistPerson();
		mockMvc.perform(post("/api/gdpr/disclosures").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"personUid\":\"" + uid + "\",\"recipient\":\"Socialna poistovna\","
						+ "\"recipientType\":\"state_agency\",\"legalBasis\":\"art6_1c\","
						+ "\"disclosedFields\":\"name,address\"}"))
				.andExpect(status().isCreated());

		// the Art. 15 export carries every section: current composite, full histories, vital
		// records where the person is a subject, consents, disclosures, and the own access log.
		mockMvc.perform(get("/api/gdpr/persons/{uid}/export", uid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uid").value(uid))
				.andExpect(jsonPath("$.person.uid").value(uid))
				.andExpect(jsonPath("$.detailHistory.length()")
						.value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.addressHistory").isArray())
				.andExpect(jsonPath("$.religionHistory").isArray())
				.andExpect(jsonPath("$.births").isArray())
				.andExpect(jsonPath("$.marriages").isArray())
				.andExpect(jsonPath("$.deaths").isArray())
				.andExpect(jsonPath("$.consents").isArray())
				.andExpect(jsonPath("$.disclosures").isArray())
				.andExpect(jsonPath("$.accessLog.length()")
						.value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

		// original disclosure + the export-as-disclosure
		mockMvc.perform(get("/api/gdpr/disclosures").param("personUid", uid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@WithUserDetails("dpo")
	void searchLogsAccessPerHit() throws Exception {
		String uid = persistPerson();
		mockMvc.perform(get("/api/persons").param("familyName", "Novak")
				.header("X-Access-Purpose", "search-check").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertTrue(
				accessLogRepository.findByPersonUidOrderByAccessedAtDesc(uid).stream()
						.anyMatch(a -> "search-check".equals(a.getPurpose())),
				"a search must record one access-log row per matched person");
	}

	@Test
	@WithUserDetails("dpo")
	void auditEventsAccessLogAndChainVerification() throws Exception {
		String uid = persistPerson();
		createDsr(uid); // a write -> an audit event
		mockMvc.perform(get("/api/persons/{uid}", uid).header("X-Access-Purpose", "oversight")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()); // a read -> an access-log entry

		mockMvc.perform(get("/api/audit/events").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
		mockMvc.perform(get("/api/audit/access-log").param("personUid", uid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
		mockMvc.perform(get("/api/audit/chain/verification").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.intact").value(true));
	}

	@Test
	@WithUserDetails("user")
	void clerkCannotReadAudit() throws Exception {
		mockMvc.perform(get("/api/audit/events").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithUserDetails("citizen")
	void citizenSeesOwnDataViaMe() throws Exception {
		mockMvc.perform(get("/api/me").header("X-Access-Purpose", "self-service").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.detail.familyName").value("Portalovy"));
		mockMvc.perform(get("/api/me/access-log").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@WithUserDetails("user")
	void nonCitizenCannotUseMe() throws Exception {
		mockMvc.perform(get("/api/me").header("X-Access-Purpose", "x").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	// --- helpers -------------------------------------------------------------

	private Long createDsr(String uid) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/gdpr/requests").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"personUid\":\"" + uid + "\",\"requesterRef\":\"REQ-1\",\"requestType\":\"access\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).asLong();
	}

	private Long createConsent(String uid) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/gdpr/consents").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"personUid\":\"" + uid + "\",\"purpose\":\"religion\",\"givenVia\":\"in_person\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).asLong();
	}

	private String persistPerson() {
		Person person = new Person();
		person.setPersonUid(personUidService.generate());
		person.setAnonymized(false);
		person.setCreatedAt(LocalDateTime.now());
		person.setCreatedBy("admin");
		personRepository.save(person);
		PersonDetail detail = new PersonDetail();
		detail.setPersonUid(person.getPersonUid());
		detail.setGivenNames("Jan");
		detail.setFamilyName("Novak");
		detail.setSex("M");
		detail.setDateOfBirth(LocalDate.parse("1990-01-01"));
		detail.setDateOfBirthPrecision("day");
		detail.setCitizenship("703");
		detail.setValidFrom(LocalDate.parse("2000-01-01"));
		detail.setRecordedAt(LocalDateTime.now());
		detail.setRecordedBy("admin");
		detailRepository.save(detail);
		return person.getPersonUid();
	}

	private void persistReligion(String uid, Long consentId) {
		PersonReligion religion = new PersonReligion();
		religion.setPersonUid(uid);
		religion.setReligionCode("RIMKAT");
		religion.setLegalBasis(CONSENT_BASIS);
		religion.setConsentId(consentId);
		religion.setValidFrom(LocalDate.parse("2000-01-01"));
		religion.setRecordedAt(LocalDateTime.now());
		religion.setRecordedBy("admin");
		religionRepository.save(religion);
	}
}
