package sk.foundation.techdemo.registry.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import sk.foundation.techdemo.BaseIT;
import sk.foundation.techdemo.registry.PersonDetail;
import sk.foundation.techdemo.registry.PersonDetailRepository;

/**
 * End-to-end coverage of the registry person API on a dedicated in-memory database: create +
 * composite read (T2), detail/address versioning (supersede), religion consent constraint (T5),
 * the X-Access-Purpose requirement, and the authorization matrix.
 */
@WithMockUser(username = "user", roles = "CLERK")
@TestPropertySource(properties =
		"spring.datasource.url=jdbc:h2:mem:registrypersonit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class RegistryPersonApiIT extends BaseIT {

	private static final String API = "/api/persons";
	private static final String PURPOSE = "X-Access-Purpose";

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PersonDetailRepository detailRepository;

	@Test
	void createThenReadComposite() throws Exception {
		String uid = createPerson("Jan", "Novak");
		mockMvc.perform(get(API + "/{uid}", uid).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uid").value(uid))
				.andExpect(jsonPath("$.detail.givenNames").value("Jan"))
				.andExpect(jsonPath("$.detail.familyName").value("Novak"))
				.andExpect(jsonPath("$.deceased").value(false));
	}

	@Test
	void readWithoutAccessPurposeIsRejected() throws Exception {
		String uid = createPerson("Eva", "Mala");
		mockMvc.perform(get(API + "/{uid}", uid).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void addingDetailSupersedesPrevious() throws Exception {
		String uid = createPerson("Peter", "Stary");
		mockMvc.perform(post(API + "/{uid}/details", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"givenNames":"Peter","familyName":"Novy","sex":"M","dateOfBirth":"1980-01-01",
						 "citizenship":"703","validFrom":"2024-01-01","changeReason":"name change"}"""))
				.andExpect(status().isCreated());

		// supersede-and-close yields THREE history rows: the transaction-time superseded original,
		// the bounded copy carrying the old belief until the change date, and the new open version.
		MvcResult res = mockMvc.perform(get(API + "/{uid}/details", uid).header(PURPOSE, "history")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andReturn();
		JsonNode rows = objectMapper.readTree(res.getResponse().getContentAsString());
		long open = 0;
		long closedCopy = 0;
		long superseded = 0;
		for (JsonNode n : rows) {
			if (n.hasNonNull("supersededAt")) {
				superseded++;
			} else if (!n.hasNonNull("validTo")) {
				open++;
			} else if ("2024-01-01".equals(n.get("validTo").asString())) {
				closedCopy++;
			}
		}
		assertEquals(1, open, "one open current-belief version");
		assertEquals(1, closedCopy, "one bounded copy of the old belief ending on the change date");
		assertEquals(1, superseded, "the original open row is transaction-time superseded");
	}

	@Test
	void permanentAddressSupersedesPrevious() throws Exception {
		String uid = createPerson("Anna", "Adresna");
		postAddress(uid, "permanent", "Hlavna 1", "2000-01-01");
		postAddress(uid, "permanent", "Nova 2", "2020-01-01");

		// current-belief timeline now has two rows (sorted by valid-from): the bounded old address
		// [2000-01-01, 2020-01-01) then the new open one.
		mockMvc.perform(get(API + "/{uid}/addresses", uid).header(PURPOSE, "lookup")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].street").value("Hlavna 1"))
				.andExpect(jsonPath("$[0].validTo").value("2020-01-01"))
				.andExpect(jsonPath("$[1].street").value("Nova 2"));

		// the composite still shows only the address valid in valid-time today
		mockMvc.perform(get(API + "/{uid}", uid).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.addresses.length()").value(1))
				.andExpect(jsonPath("$.addresses[0].street").value("Nova 2"));
	}

	@Test
	void consentBasedReligionRequiresConsent() throws Exception {
		String uid = createPerson("Igor", "Verny");
		mockMvc.perform(post(API + "/{uid}/religion", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"religionCode":"RIMKAT","legalBasis":"art9_2a_consent","validFrom":"2000-01-01"}"""))
				.andExpect(status().isConflict());

		mockMvc.perform(post(API + "/{uid}/religion", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"religionCode":"RIMKAT","legalBasis":"art9_2a_consent","validFrom":"2000-01-01",
						 "consentPurpose":"religion_registration","consentGivenVia":"in_person"}"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get(API + "/{uid}/religion", uid).header(PURPOSE, "lookup")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].religionCode").value("RIMKAT"));
	}

	@Test
	void pdfExtractIsGenerated() throws Exception {
		String uid = createPerson("Karol", "Pdfovy");
		mockMvc.perform(get(API + "/{uid}/pdf", uid).header(PURPOSE, "extract")
				.accept(MediaType.APPLICATION_PDF))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));
	}

	@Test
	@WithMockUser(username = "dpo", roles = "DPO_OFFICER")
	void dpoMayReadButNotCreate() throws Exception {
		mockMvc.perform(get(API).header(PURPOSE, "oversight").param("familyName", "x")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		mockMvc.perform(post(API).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(personJson("X", "Y")))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "agency", roles = "AGENCY_READER")
	void agencyCannotReachPersonApi() throws Exception {
		mockMvc.perform(get(API).header(PURPOSE, "x").param("familyName", "x").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "citizen", roles = "CITIZEN")
	void citizenCannotUseClerkApi() throws Exception {
		mockMvc.perform(post(API).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(personJson("X", "Y")))
				.andExpect(status().isForbidden());
	}

	@Test
	void closedRangeOverlappingOpenDetailIsRejected() throws Exception {
		String uid = createPerson("Ova", "Lap");
		// a bounded version inside the open [dob, +inf) range overlaps in valid time -> 409
		mockMvc.perform(post(API + "/{uid}/details", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"givenNames":"Ova","familyName":"Lap2","sex":"M","dateOfBirth":"1980-01-01",
						 "citizenship":"703","validFrom":"1990-01-01","validTo":"2000-01-01"}"""))
				.andExpect(status().isConflict());
		// history unchanged: still the single original version
		mockMvc.perform(get(API + "/{uid}/details", uid).header(PURPOSE, "history").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void overlappingSameTypeAddressRejectedButOtherTypeAllowed() throws Exception {
		String uid = createPerson("Bob", "Byt");
		postAddress(uid, "permanent", "Hlavna 1", "2000-01-01");
		// same type, overlapping in valid time -> 409
		mockMvc.perform(post(API + "/{uid}/addresses", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"street\":\"X\",\"countryCode\":\"703\",\"foreignAddress\":\"X\","
						+ "\"addressType\":\"permanent\",\"validFrom\":\"2010-01-01\",\"validTo\":\"2015-01-01\"}"))
				.andExpect(status().isConflict());
		// different type, overlapping in valid time -> allowed (per-type key)
		mockMvc.perform(post(API + "/{uid}/addresses", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"street\":\"Y\",\"countryCode\":\"703\",\"foreignAddress\":\"Y\","
						+ "\"addressType\":\"temporary\",\"validFrom\":\"2010-01-01\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void historicalClosedDetailPreservesCurrent() throws Exception {
		String uid = createPerson("Cyril", "Stor");
		// a bounded past version that does not overlap the open [1980, +inf) range -> 201
		mockMvc.perform(post(API + "/{uid}/details", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"givenNames":"Cyril","familyName":"Historic","sex":"M","dateOfBirth":"1980-01-01",
						 "citizenship":"703","validFrom":"1970-01-01","validTo":"1975-01-01"}"""))
				.andExpect(status().isCreated());
		// both versions are current-belief (nothing superseded)
		mockMvc.perform(get(API + "/{uid}/details", uid).header(PURPOSE, "history").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
		// composite still shows the version valid today (the original open range)
		mockMvc.perform(get(API + "/{uid}", uid).header(PURPOSE, "lookup").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.detail.familyName").value("Stor"));
	}

	@Test
	void pointInTimeSelectionAcrossSupersededTimeline() throws Exception {
		String uid = createPerson("Dana", "Predtym");
		mockMvc.perform(post(API + "/{uid}/details", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"givenNames":"Dana","familyName":"Potom","sex":"F","dateOfBirth":"1980-01-01",
						 "citizenship":"703","validFrom":"2024-01-01"}"""))
				.andExpect(status().isCreated());

		// T4: the supersede-produced timeline supports a half-open point-in-time pick at repo level
		List<PersonDetail> timeline = detailRepository.findByPersonUidAndSupersededAtIsNull(uid);
		assertEquals("Predtym", pickAsOf(timeline, LocalDate.parse("2000-01-01")).getFamilyName());
		assertEquals("Potom", pickAsOf(timeline, LocalDate.parse("2025-01-01")).getFamilyName());
	}

	@Test
	void secondOpenReligionSupersedesWithClosedCopy() throws Exception {
		String uid = createPerson("Emil", "Viera");
		postReligion(uid, "RIMKAT", "2000-01-01");
		postReligion(uid, "NONE", "2020-01-01");
		mockMvc.perform(get(API + "/{uid}/religion", uid).header(PURPOSE, "lookup")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].religionCode").value("RIMKAT"))
				.andExpect(jsonPath("$[0].validTo").value("2020-01-01"))
				.andExpect(jsonPath("$[1].religionCode").value("NONE"));
	}

	@Test
	void agencyReaderSeesMinimalCurrentView() throws Exception {
		String uid = createPerson("Agnesa", "Agentna");
		postAddress(uid, "permanent", "Hlavna 1", "2000-01-01");
		// AGENCY_READER gets the minimal current projection (identity + permanent address)
		mockMvc.perform(get("/api/agency/persons/{uid}", uid).with(user("agency").roles("AGENCY_READER"))
				.header(PURPOSE, "agency-check").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uid").value(uid))
				.andExpect(jsonPath("$.familyName").value("Agentna"))
				.andExpect(jsonPath("$.permanentAddress.street").value("Hlavna 1"));
	}

	@Test
	void clerkAndCitizenCannotReachAgencyApi() throws Exception {
		String uid = createPerson("Clerk", "Denied");
		// class default principal is CLERK
		mockMvc.perform(get("/api/agency/persons/{uid}", uid).header(PURPOSE, "x").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/agency/persons/{uid}", uid).with(user("cit").roles("CITIZEN"))
				.header(PURPOSE, "x").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	private static PersonDetail pickAsOf(List<PersonDetail> timeline, LocalDate asOf) {
		return timeline.stream()
				.filter(d -> !d.getValidFrom().isAfter(asOf)
						&& (d.getValidTo() == null || d.getValidTo().isAfter(asOf)))
				.findFirst().orElseThrow();
	}

	private void postReligion(String uid, String code, String validFrom) throws Exception {
		mockMvc.perform(post(API + "/{uid}/religion", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"religionCode\":\"" + code + "\",\"legalBasis\":\"art9_2a_consent\",\"validFrom\":\""
						+ validFrom + "\",\"consentPurpose\":\"religion_registration\","
						+ "\"consentGivenVia\":\"in_person\"}"))
				.andExpect(status().isCreated());
	}

	private String createPerson(String givenNames, String familyName) throws Exception {
		MvcResult result = mockMvc.perform(post(API).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(personJson(givenNames, familyName)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return body.get("uid").asString();
	}

	private void postAddress(String uid, String type, String street, String validFrom) throws Exception {
		mockMvc.perform(post(API + "/{uid}/addresses", uid).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"street\":\"" + street + "\",\"countryCode\":\"703\",\"foreignAddress\":\"" + street
						+ ", Bratislava\",\"addressType\":\"" + type + "\",\"validFrom\":\"" + validFrom + "\"}"))
				.andExpect(status().isCreated());
	}

	private static String personJson(String givenNames, String familyName) {
		return "{\"givenNames\":\"" + givenNames + "\",\"familyName\":\"" + familyName
				+ "\",\"sex\":\"M\",\"dateOfBirth\":\"1980-01-01\",\"citizenship\":\"703\"}";
	}
}
