package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.AccessLogService;
import sk.foundation.techdemo.auditlog.AuditAction;
import sk.foundation.techdemo.auditlog.AuditService;
import sk.foundation.techdemo.auditlog.api.AccessLogEntryDTO;
import sk.foundation.techdemo.auth.CurrentUser;
import sk.foundation.techdemo.infrastructure.api.ConflictException;
import sk.foundation.techdemo.infrastructure.api.PagedRequestDTO;
import sk.foundation.techdemo.infrastructure.api.PagedResultResponseDTO;
import sk.foundation.techdemo.infrastructure.crypto.NationalIdCryptoService;
import sk.foundation.techdemo.gdpr.Consent;
import sk.foundation.techdemo.gdpr.ConsentRepository;
import sk.foundation.techdemo.records.DeathRecordRepository;
import sk.foundation.techdemo.registry.Address;
import sk.foundation.techdemo.registry.AddressRepository;
import sk.foundation.techdemo.registry.BitemporalGuard;
import sk.foundation.techdemo.registry.Person;
import sk.foundation.techdemo.registry.PersonAddress;
import sk.foundation.techdemo.registry.PersonAddressRepository;
import sk.foundation.techdemo.registry.PersonDetail;
import sk.foundation.techdemo.registry.PersonDetailRepository;
import sk.foundation.techdemo.registry.PersonReligion;
import sk.foundation.techdemo.registry.PersonReligionRepository;
import sk.foundation.techdemo.registry.PersonUidService;
import sk.foundation.techdemo.registry.PortalEligibilityService;
import sk.foundation.techdemo.registry.RegistryPersonRepository;

/**
 * Application service for the civil-registry person aggregate: creation, bitemporal versioning
 * of identity details / addresses / religion, current composite reads, and search. Each write
 * follows the DESIGN §4 supersede-and-close flow — an open-ended insert starting later closes the
 * prior open row (transaction-time superseding it and re-recording the old belief as a bounded
 * copy) — then {@link BitemporalGuard} rejects any residual valid-time overlap before the new row
 * is saved. Writes hold the person-row pessimistic lock (the guard's precondition) and append to
 * the tamper-evident audit log; personal-data reads are recorded in the access log (GDPR Art. 15).
 */
@Service
@RequiredArgsConstructor
public class RegistryPersonApiService {

	private static final String SCHEMA = "REGISTRY";
	private static final String T_PERSON = "PERSON";
	private static final String T_DETAIL = "PERSON_DETAIL";
	private static final String T_ADDRESS = "PERSON_ADDRESS";
	private static final String T_RELIGION = "PERSON_RELIGION";
	private static final String DEFAULT_PRECISION = "day";
	private static final String CONSENT_BASIS = "art9_2a_consent";

	private final RegistryPersonRepository personRepository;
	private final PersonDetailRepository detailRepository;
	private final PersonAddressRepository addressLinkRepository;
	private final AddressRepository addressRepository;
	private final PersonReligionRepository religionRepository;
	private final ConsentRepository consentRepository;
	private final DeathRecordRepository deathRecordRepository;
	private final PersonUidService personUidService;
	private final NationalIdCryptoService cryptoService;
	private final BitemporalGuard bitemporalGuard;
	private final AuditService auditService;
	private final AccessLogService accessLogService;
	private final PortalEligibilityService portalEligibilityService;

	// ---------------------------------------------------------------- writes

	@Transactional
	public String createPerson(PersonCreateRequestDTO dto, String requestId) {
		String actor = CurrentUser.username();
		String uid = personUidService.generate();
		Person person = new Person();
		person.setPersonUid(uid);
		person.setAnonymized(false);
		person.setCreatedAt(LocalDateTime.now());
		person.setCreatedBy(actor);
		if (StringUtils.hasText(dto.nationalId())) {
			person.setNationalIdEnc(cryptoService.encrypt(dto.nationalId()));
			person.setNationalIdHmac(cryptoService.hmac(dto.nationalId()));
		}
		personRepository.save(person);

		LocalDate validFrom = dto.validFrom() != null ? dto.validFrom() : dto.dateOfBirth();
		insertDetail(uid, toDetailFromCreate(dto), validFrom, null, null, actor);
		audit(T_PERSON, AuditAction.INSERT, null, json("personUid", uid), requestId);
		return uid;
	}

	@Transactional
	public Long addDetail(String uid, PersonDetailRequestDTO dto, String requestId) {
		lockPerson(uid);
		String actor = CurrentUser.username();
		PersonDetail detail = new PersonDetail();
		detail.setGivenNames(dto.givenNames());
		detail.setFamilyName(dto.familyName());
		detail.setBirthSurname(dto.birthSurname());
		detail.setSex(dto.sex());
		detail.setDateOfBirth(dto.dateOfBirth());
		detail.setDateOfBirthPrecision(precision(dto.dateOfBirthPrecision()));
		detail.setPlaceOfBirthMunicipality(dto.placeOfBirthMunicipality());
		detail.setPlaceOfBirthForeign(dto.placeOfBirthForeign());
		detail.setCitizenship(dto.citizenship());
		detail.setMaritalStatusCode(dto.maritalStatusCode());
		detail.setExistenceStatusCode(dto.existenceStatusCode());
		detail.setTitlesBefore(dto.titlesBefore());
		detail.setTitlesAfter(dto.titlesAfter());
		Long id = insertDetail(uid, detail, dto.validFrom(), dto.validTo(), dto.changeReason(), actor);
		audit(T_DETAIL, AuditAction.INSERT, null, json("detailId", String.valueOf(id)), requestId);
		return id;
	}

	@Transactional
	public Long addAddress(String uid, AddressRequestDTO dto, String requestId) {
		lockPerson(uid);
		String actor = CurrentUser.username();

		assertValidRange(dto.validFrom(), dto.validTo());

		Address address = new Address();
		address.setAddressId(UUID.randomUUID().toString());
		address.setStreet(dto.street());
		address.setHouseNumber(dto.houseNumber());
		address.setPostalCode(dto.postalCode());
		address.setMunicipalityCode(dto.municipalityCode());
		address.setMunicipalityPartCode(dto.municipalityPartCode());
		address.setCountryCode(dto.countryCode());
		address.setForeignAddress(dto.foreignAddress());
		addressRepository.save(address);

		LocalDateTime now = LocalDateTime.now();
		List<PersonAddress> currentOfType = addressLinkRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.filter(a -> dto.addressType().equals(a.getAddressType()))
				.toList();
		PersonAddress correctionTarget = dto.validTo() == null
				? closeOpenAddress(currentOfType, dto.validFrom(), now, actor) : null;
		bitemporalGuard.assertAddressInsertable(uid, dto.addressType(), dto.validFrom(), dto.validTo());

		PersonAddress link = new PersonAddress();
		link.setPersonUid(uid);
		link.setAddressId(address.getAddressId());
		link.setAddressType(dto.addressType());
		link.setValidFrom(dto.validFrom());
		link.setValidTo(dto.validTo());
		link.setRecordedAt(now);
		link.setRecordedBy(actor);
		link.setChangeReason(dto.changeReason());
		Long id = addressLinkRepository.save(link).getPersonAddressId();
		if (correctionTarget != null) {
			correctionTarget.setSupersededBy(id);
			addressLinkRepository.save(correctionTarget);
		}
		audit(T_ADDRESS, AuditAction.INSERT, null, json("personAddressId", String.valueOf(id)), requestId);
		return id;
	}

	@Transactional
	public Long addReligion(String uid, ReligionRequestDTO dto, String requestId) {
		lockPerson(uid);
		String actor = CurrentUser.username();
		Long consentId = null;
		if (CONSENT_BASIS.equals(dto.legalBasis())) {
			if (!StringUtils.hasText(dto.consentPurpose()) || !StringUtils.hasText(dto.consentGivenVia())) {
				throw new ConflictException("Consent-based religion requires consentPurpose and consentGivenVia");
			}
			Consent consent = new Consent();
			consent.setPersonUid(uid);
			consent.setPurpose(dto.consentPurpose());
			consent.setGivenAt(LocalDateTime.now());
			consent.setGivenVia(dto.consentGivenVia());
			consent.setRecordedBy(actor);
			consentId = consentRepository.save(consent).getConsentId();
		}
		assertValidRange(dto.validFrom(), dto.validTo());
		LocalDateTime now = LocalDateTime.now();
		List<PersonReligion> current = religionRepository.findByPersonUidAndSupersededAtIsNull(uid);
		PersonReligion correctionTarget = dto.validTo() == null
				? closeOpenReligion(current, dto.validFrom(), now, actor) : null;
		bitemporalGuard.assertReligionInsertable(uid, dto.validFrom(), dto.validTo());

		PersonReligion religion = new PersonReligion();
		religion.setPersonUid(uid);
		religion.setReligionCode(dto.religionCode());
		religion.setLegalBasis(dto.legalBasis());
		religion.setConsentId(consentId);
		religion.setValidFrom(dto.validFrom());
		religion.setValidTo(dto.validTo());
		religion.setRecordedAt(now);
		religion.setRecordedBy(actor);
		religion.setChangeReason(dto.changeReason());
		Long id = religionRepository.save(religion).getPersonReligionId();
		if (correctionTarget != null) {
			correctionTarget.setSupersededBy(id);
			religionRepository.save(correctionTarget);
		}
		audit(T_RELIGION, AuditAction.INSERT, null, json("personReligionId", String.valueOf(id)), requestId);
		return id;
	}

	// ---------------------------------------------------------------- reads

	@Transactional(readOnly = true)
	public PersonCompositeResponseDTO getComposite(String uid, String purpose, String requestId) {
		ensureExists(uid);
		LocalDate today = LocalDate.now();
		DetailResponseDTO detail = currentDetail(uid, today).map(RegistryPersonApiService::toDetailDto).orElse(null);
		List<AddressResponseDTO> addresses = addressLinkRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.filter(a -> currentInValidTime(a.getValidFrom(), a.getValidTo(), today))
				.map(this::toAddressDto)
				.toList();
		boolean deceased = deathRecordRepository.existsByPersonUidAndSupersededAtIsNull(uid);
		logAccess(uid, purpose, "detail,address", requestId);
		return new PersonCompositeResponseDTO(uid, deceased, detail, addresses);
	}

	@Transactional(readOnly = true)
	public List<DetailResponseDTO> getDetailHistory(String uid, String purpose, String requestId) {
		ensureExists(uid);
		logAccess(uid, purpose, "detail-history", requestId);
		return detailRepository.findByPersonUidOrderByRecordedAtDesc(uid).stream()
				.map(RegistryPersonApiService::toDetailDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AddressResponseDTO> getAddresses(String uid, String purpose, String requestId) {
		ensureExists(uid);
		logAccess(uid, purpose, "address", requestId);
		return addressLinkRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.sorted(Comparator.comparing(PersonAddress::getValidFrom)
						.thenComparing(PersonAddress::getPersonAddressId))
				.map(this::toAddressDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ReligionResponseDTO> getReligion(String uid, String purpose, String requestId) {
		ensureExists(uid);
		logAccess(uid, purpose, "religion", requestId);
		return religionRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.sorted(Comparator.comparing(PersonReligion::getValidFrom)
						.thenComparing(PersonReligion::getPersonReligionId))
				.map(RegistryPersonApiService::toReligionDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public PagedResultResponseDTO<PersonSummaryDTO> search(String familyName, PagedRequestDTO paging,
			String purpose, String requestId) {
		Pageable pageable = PageRequest.of(paging.getPageStart(), paging.getPageSize());
		// Only current-belief versions valid today: closed history copies carry the (possibly
		// stale) name they held at the time, so a plain family-name match would surface stale hits.
		var page = detailRepository.searchCurrent(
				familyName == null ? "" : familyName, LocalDate.now(), pageable);
		List<PersonSummaryDTO> items = page.getContent().stream()
				.map(d -> new PersonSummaryDTO(d.getPersonUid(), d.getGivenNames(), d.getFamilyName(),
						d.getDateOfBirth(), d.getSex()))
				.toList();
		// GDPR Art. 15: a search surfaces each matched person's data — log one access row per hit.
		page.getContent().forEach(d -> logAccess(d.getPersonUid(), purpose, "search", requestId));
		return new PagedResultResponseDTO<>(page.getTotalElements(), items);
	}

	// ---------------------------------------------------------- agency (T9)

	/**
	 * Minimal current-state view for the AGENCY_READER role (the {@code V_PERSON_CURRENT} column
	 * set): current identity detail, sex, existence status, and the current permanent address. No
	 * Art. 9 data, history, or vital records. Access-logged like any personal-data read.
	 */
	@Transactional(readOnly = true)
	public AgencyPersonDTO agencyView(String uid, String purpose, String requestId) {
		ensureExists(uid);
		LocalDate today = LocalDate.now();
		PersonDetail detail = currentDetail(uid, today)
				.orElseThrow(() -> new EntityNotFoundException("No current identity detail for person: " + uid));
		AddressResponseDTO permanent = addressLinkRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.filter(a -> "permanent".equals(a.getAddressType()))
				.filter(a -> currentInValidTime(a.getValidFrom(), a.getValidTo(), today))
				.findFirst()
				.map(this::toAddressDto)
				.orElse(null);
		logAccess(uid, purpose, "agency-minimal", requestId);
		return new AgencyPersonDTO(uid, detail.getGivenNames(), detail.getFamilyName(),
				detail.getDateOfBirth(), detail.getSex(), detail.getExistenceStatusCode(), permanent);
	}

	// --------------------------------------------------- Art. 15 export slices
	// Full histories for the GDPR export. No access-logging here — the export operation records a
	// single disclosure + access-log entry in GdprApiService; these methods only assemble data.

	@Transactional(readOnly = true)
	public List<DetailResponseDTO> detailHistoryForExport(String uid) {
		return detailRepository.findByPersonUidOrderByRecordedAtDesc(uid).stream()
				.map(RegistryPersonApiService::toDetailDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AddressResponseDTO> addressHistoryForExport(String uid) {
		return addressLinkRepository.findByPersonUidOrderByRecordedAtDesc(uid).stream()
				.map(this::toAddressDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ReligionResponseDTO> religionHistoryForExport(String uid) {
		return religionRepository.findByPersonUidOrderByRecordedAtDesc(uid).stream()
				.map(RegistryPersonApiService::toReligionDto)
				.toList();
	}

	// ------------------------------------------------------------ citizen /me

	/** Own composite view for the authenticated, portal-eligible citizen. */
	@Transactional(readOnly = true)
	public PersonCompositeResponseDTO getMe(String purpose, String requestId) {
		return getComposite(ownEligibleUid(), purpose, requestId);
	}

	/** Own read-access log (GDPR Art. 15) for the authenticated citizen. */
	@Transactional(readOnly = true)
	public List<AccessLogEntryDTO> meAccessLog() {
		String uid = ownEligibleUid();
		return accessLogService.forPerson(uid).stream()
				.map(a -> new AccessLogEntryDTO(a.getAccessId(), a.getAccessedAt(), a.getActor(),
						a.getPersonUid(), a.getPurpose(), a.getLegalBasis(), a.getFields(), a.getRequestId()))
				.toList();
	}

	private String ownEligibleUid() {
		String uid = CurrentUser.personUid();
		if (uid == null || !portalEligibilityService.isEligible(uid)) {
			throw new AccessDeniedException("Citizen portal access is not available for this account");
		}
		return uid;
	}

	// ---------------------------------------------------------------- internals

	private Long insertDetail(String uid, PersonDetail template, LocalDate validFrom, LocalDate validTo,
			String changeReason, String actor) {
		assertValidRange(validFrom, validTo);
		LocalDateTime now = LocalDateTime.now();
		List<PersonDetail> current = detailRepository.findByPersonUidAndSupersededAtIsNull(uid);
		PersonDetail correctionTarget = validTo == null ? closeOpenDetail(current, validFrom, now, actor) : null;
		bitemporalGuard.assertDetailInsertable(uid, validFrom, validTo);

		template.setPersonUid(uid);
		template.setValidFrom(validFrom);
		template.setValidTo(validTo);
		template.setRecordedAt(now);
		template.setRecordedBy(actor);
		template.setChangeReason(changeReason);
		Long id = detailRepository.save(template).getDetailId();
		if (correctionTarget != null) {
			correctionTarget.setSupersededBy(id);
			detailRepository.save(correctionTarget);
		}
		return id;
	}

	/**
	 * DESIGN §4 supersede-and-close for an open-ended insert: if an open (unbounded) current row
	 * exists and the new row starts <em>after</em> it, re-record the old belief as a bounded copy
	 * (validity ending where the new row starts) and transaction-time supersede the original; if
	 * they start on the same day it is a correction — supersede the original in place (no empty
	 * copy) and return it so its {@code supersededBy} can be back-linked to the new row. A new row
	 * starting <em>before</em> the open row is left for the guard to reject. Rows are flushed so the
	 * guard query sees the post-truncation state.
	 */
	private PersonDetail closeOpenDetail(List<PersonDetail> current, LocalDate newValidFrom,
			LocalDateTime now, String actor) {
		PersonDetail open = current.stream().filter(d -> d.getValidTo() == null).findFirst().orElse(null);
		if (open == null) {
			return null;
		}
		if (open.getValidFrom().isBefore(newValidFrom)) {
			PersonDetail copy = closedDetailCopy(open, newValidFrom, now, actor);
			detailRepository.saveAndFlush(copy);
			open.setSupersededAt(now);
			open.setSupersededBy(copy.getDetailId());
			detailRepository.saveAndFlush(open);
			return null;
		}
		if (open.getValidFrom().isEqual(newValidFrom)) {
			open.setSupersededAt(now);
			detailRepository.saveAndFlush(open);
			return open;
		}
		return null;
	}

	private PersonAddress closeOpenAddress(List<PersonAddress> currentOfType, LocalDate newValidFrom,
			LocalDateTime now, String actor) {
		PersonAddress open = currentOfType.stream().filter(a -> a.getValidTo() == null).findFirst().orElse(null);
		if (open == null) {
			return null;
		}
		if (open.getValidFrom().isBefore(newValidFrom)) {
			PersonAddress copy = closedAddressCopy(open, newValidFrom, now, actor);
			addressLinkRepository.saveAndFlush(copy);
			open.setSupersededAt(now);
			open.setSupersededBy(copy.getPersonAddressId());
			addressLinkRepository.saveAndFlush(open);
			return null;
		}
		if (open.getValidFrom().isEqual(newValidFrom)) {
			open.setSupersededAt(now);
			addressLinkRepository.saveAndFlush(open);
			return open;
		}
		return null;
	}

	private PersonReligion closeOpenReligion(List<PersonReligion> current, LocalDate newValidFrom,
			LocalDateTime now, String actor) {
		PersonReligion open = current.stream().filter(r -> r.getValidTo() == null).findFirst().orElse(null);
		if (open == null) {
			return null;
		}
		if (open.getValidFrom().isBefore(newValidFrom)) {
			PersonReligion copy = closedReligionCopy(open, newValidFrom, now, actor);
			religionRepository.saveAndFlush(copy);
			open.setSupersededAt(now);
			open.setSupersededBy(copy.getPersonReligionId());
			religionRepository.saveAndFlush(open);
			return null;
		}
		if (open.getValidFrom().isEqual(newValidFrom)) {
			open.setSupersededAt(now);
			religionRepository.saveAndFlush(open);
			return open;
		}
		return null;
	}

	private static PersonDetail closedDetailCopy(PersonDetail open, LocalDate newValidTo,
			LocalDateTime now, String actor) {
		PersonDetail c = new PersonDetail();
		c.setPersonUid(open.getPersonUid());
		c.setGivenNames(open.getGivenNames());
		c.setFamilyName(open.getFamilyName());
		c.setBirthSurname(open.getBirthSurname());
		c.setSex(open.getSex());
		c.setDateOfBirth(open.getDateOfBirth());
		c.setDateOfBirthPrecision(open.getDateOfBirthPrecision());
		c.setPlaceOfBirthMunicipality(open.getPlaceOfBirthMunicipality());
		c.setPlaceOfBirthForeign(open.getPlaceOfBirthForeign());
		c.setCitizenship(open.getCitizenship());
		c.setMaritalStatusCode(open.getMaritalStatusCode());
		c.setExistenceStatusCode(open.getExistenceStatusCode());
		c.setTitlesBefore(open.getTitlesBefore());
		c.setTitlesAfter(open.getTitlesAfter());
		c.setValidFrom(open.getValidFrom());
		c.setValidTo(newValidTo);
		c.setRecordedAt(now);
		c.setRecordedBy(actor);
		c.setChangeReason(open.getChangeReason());
		return c;
	}

	private static PersonAddress closedAddressCopy(PersonAddress open, LocalDate newValidTo,
			LocalDateTime now, String actor) {
		PersonAddress c = new PersonAddress();
		c.setPersonUid(open.getPersonUid());
		c.setAddressId(open.getAddressId());
		c.setAddressType(open.getAddressType());
		c.setValidFrom(open.getValidFrom());
		c.setValidTo(newValidTo);
		c.setRecordedAt(now);
		c.setRecordedBy(actor);
		c.setChangeReason(open.getChangeReason());
		return c;
	}

	private static PersonReligion closedReligionCopy(PersonReligion open, LocalDate newValidTo,
			LocalDateTime now, String actor) {
		PersonReligion c = new PersonReligion();
		c.setPersonUid(open.getPersonUid());
		c.setReligionCode(open.getReligionCode());
		c.setLegalBasis(open.getLegalBasis());
		c.setConsentId(open.getConsentId());
		c.setValidFrom(open.getValidFrom());
		c.setValidTo(newValidTo);
		c.setRecordedAt(now);
		c.setRecordedBy(actor);
		c.setChangeReason(open.getChangeReason());
		return c;
	}

	private static void assertValidRange(LocalDate validFrom, LocalDate validTo) {
		if (validTo != null && !validTo.isAfter(validFrom)) {
			throw new ConflictException("validTo must be after validFrom");
		}
	}

	private void lockPerson(String uid) {
		personRepository.lockByPersonUid(uid)
				.orElseThrow(() -> new EntityNotFoundException("Person not found: " + uid));
	}

	private void ensureExists(String uid) {
		if (!personRepository.existsById(uid)) {
			throw new EntityNotFoundException("Person not found: " + uid);
		}
	}

	private Optional<PersonDetail> currentDetail(String uid, LocalDate today) {
		return detailRepository.findByPersonUidAndSupersededAtIsNull(uid).stream()
				.filter(d -> currentInValidTime(d.getValidFrom(), d.getValidTo(), today))
				.findFirst();
	}

	private void logAccess(String uid, String purpose, String fields, String requestId) {
		accessLogService.record(CurrentUser.username(), uid, purpose, null, fields, requestId);
	}

	private void audit(String table, AuditAction action, String oldRow, String newRow, String requestId) {
		auditService.record(SCHEMA, table, action, oldRow, newRow, CurrentUser.username(), CurrentUser.role(),
				requestId, null);
	}

	private static PersonDetail toDetailFromCreate(PersonCreateRequestDTO dto) {
		PersonDetail d = new PersonDetail();
		d.setGivenNames(dto.givenNames());
		d.setFamilyName(dto.familyName());
		d.setBirthSurname(dto.birthSurname());
		d.setSex(dto.sex());
		d.setDateOfBirth(dto.dateOfBirth());
		d.setDateOfBirthPrecision(dto.dateOfBirthPrecision() == null ? DEFAULT_PRECISION : dto.dateOfBirthPrecision());
		d.setPlaceOfBirthMunicipality(dto.placeOfBirthMunicipality());
		d.setPlaceOfBirthForeign(dto.placeOfBirthForeign());
		d.setCitizenship(dto.citizenship());
		d.setMaritalStatusCode(dto.maritalStatusCode());
		d.setTitlesBefore(dto.titlesBefore());
		d.setTitlesAfter(dto.titlesAfter());
		return d;
	}

	private static String precision(String value) {
		return value == null ? DEFAULT_PRECISION : value;
	}

	private static boolean currentInValidTime(LocalDate from, LocalDate to, LocalDate today) {
		return !from.isAfter(today) && (to == null || to.isAfter(today));
	}

	private static String json(String key, String value) {
		return "{\"" + key + "\":\"" + value + "\"}";
	}

	private static DetailResponseDTO toDetailDto(PersonDetail d) {
		return new DetailResponseDTO(d.getDetailId(), d.getGivenNames(), d.getFamilyName(), d.getBirthSurname(),
				d.getSex(), d.getDateOfBirth(), d.getDateOfBirthPrecision(), d.getPlaceOfBirthMunicipality(),
				d.getPlaceOfBirthForeign(), d.getCitizenship(), d.getMaritalStatusCode(), d.getExistenceStatusCode(),
				d.getTitlesBefore(), d.getTitlesAfter(), d.getValidFrom(), d.getValidTo(), d.getRecordedAt(),
				d.getSupersededAt());
	}

	private AddressResponseDTO toAddressDto(PersonAddress link) {
		Address a = addressRepository.findById(link.getAddressId()).orElse(new Address());
		return new AddressResponseDTO(link.getPersonAddressId(), link.getAddressType(), a.getStreet(),
				a.getHouseNumber(), a.getPostalCode(), a.getMunicipalityCode(), a.getMunicipalityPartCode(),
				a.getCountryCode(), a.getForeignAddress(), link.getValidFrom(), link.getValidTo(),
				link.getSupersededAt());
	}

	private static ReligionResponseDTO toReligionDto(PersonReligion r) {
		return new ReligionResponseDTO(r.getPersonReligionId(), r.getReligionCode(), r.getLegalBasis(),
				r.getConsentId(), r.getValidFrom(), r.getValidTo(), r.getSupersededAt());
	}
}
