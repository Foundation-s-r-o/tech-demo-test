package sk.foundation.techdemo.records.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.AccessLogService;
import sk.foundation.techdemo.auditlog.AuditAction;
import sk.foundation.techdemo.auditlog.AuditService;
import sk.foundation.techdemo.auth.CurrentUser;
import sk.foundation.techdemo.auth.Role;
import sk.foundation.techdemo.infrastructure.api.ConflictException;
import sk.foundation.techdemo.records.BirthRecord;
import sk.foundation.techdemo.records.BirthRecordRepository;
import sk.foundation.techdemo.records.DeathMedical;
import sk.foundation.techdemo.records.DeathMedicalRepository;
import sk.foundation.techdemo.records.DeathRecord;
import sk.foundation.techdemo.records.DeathRecordRepository;
import sk.foundation.techdemo.records.MarriageDissolution;
import sk.foundation.techdemo.records.MarriageDissolutionRepository;
import sk.foundation.techdemo.records.MarriageRecord;
import sk.foundation.techdemo.records.MarriageRecordRepository;

/**
 * Application service for append-only vital records. Registration is scoped to the clerk's own
 * office (supervisors work nationwide); the registry number {@code (office, year, entry)} is unique
 * among current versions; amendments supersede the prior version and require a reason + supervisor
 * approval. Writes append to the audit log; reads are access-logged for the record subject.
 */
@Service
@RequiredArgsConstructor
public class VitalRecordsApiService {

	private static final String SCHEMA = "REGISTRY";
	private static final String T_BIRTH = "BIRTH_RECORD";
	private static final String T_MARRIAGE = "MARRIAGE_RECORD";
	private static final String T_DEATH = "DEATH_RECORD";
	private static final String T_DISSOLUTION = "MARRIAGE_DISSOLUTION";
	private static final String T_MEDICAL = "DEATH_MEDICAL";
	private static final String CLERK_AUTHORITY = Role.CLERK.authority();
	private static final String K_RECORD_ID = "recordId";
	private static final String LABEL_MARRIAGE = "Marriage record";
	private static final String LABEL_DEATH = "Death record";

	private final BirthRecordRepository birthRepository;
	private final MarriageRecordRepository marriageRepository;
	private final MarriageDissolutionRepository dissolutionRepository;
	private final DeathRecordRepository deathRepository;
	private final DeathMedicalRepository deathMedicalRepository;
	private final AuditService auditService;
	private final AccessLogService accessLogService;

	// ---------------------------------------------------------------- births

	@Transactional
	public String registerBirth(BirthRequestDTO dto, String requestId) {
		assertOfficeAllowed(dto.officeCode());
		assertRegistryNumberFree(!birthRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo()).isEmpty());
		BirthRecord record = toBirth(dto, UUID.randomUUID().toString(), null, null, null);
		save(record);
		audit(T_BIRTH, AuditAction.INSERT, json(K_RECORD_ID, record.getRecordId()), requestId);
		logAccess(dto.childUid(), "birth-registration", requestId);
		return record.getRecordId();
	}

	@Transactional
	public String amendBirth(String recordId, BirthRequestDTO dto, String requestId) {
		BirthRecord previous = birthRepository.findById(recordId)
				.orElseThrow(() -> notFound("Birth record", recordId));
		assertAmendable(previous.getSupersededAt());
		requireReason(dto.amendmentReason());
		assertRegistryNumberFree(birthRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo())
				.stream().anyMatch(r -> !r.getRecordId().equals(previous.getRecordId())));
		previous.setSupersededAt(LocalDateTime.now());
		birthRepository.save(previous);
		BirthRecord amended = toBirth(dto, UUID.randomUUID().toString(), previous.getRecordId(),
				dto.amendmentReason(), CurrentUser.username());
		save(amended);
		audit(T_BIRTH, AuditAction.UPDATE, json(K_RECORD_ID, amended.getRecordId()), requestId);
		return amended.getRecordId();
	}

	@Transactional(readOnly = true)
	public BirthResponseDTO getBirth(String recordId, String purpose, String requestId) {
		BirthRecord record = birthRepository.findById(recordId)
				.orElseThrow(() -> notFound("Birth record", recordId));
		assertOfficeAllowed(record.getOfficeCode());
		logAccess(record.getChildUid(), purpose, requestId);
		return toBirthDto(record);
	}

	// -------------------------------------------------------------- marriages

	@Transactional
	public String registerMarriage(MarriageRequestDTO dto, String requestId) {
		assertOfficeAllowed(dto.officeCode());
		assertRegistryNumberFree(!marriageRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo()).isEmpty());
		MarriageRecord record = toMarriage(dto, UUID.randomUUID().toString(), null, null, null);
		save(record);
		audit(T_MARRIAGE, AuditAction.INSERT, json(K_RECORD_ID, record.getRecordId()), requestId);
		logAccess(dto.spouse1Uid(), "marriage-registration", requestId);
		return record.getRecordId();
	}

	@Transactional
	public String amendMarriage(String recordId, MarriageRequestDTO dto, String requestId) {
		MarriageRecord previous = marriageRepository.findById(recordId)
				.orElseThrow(() -> notFound(LABEL_MARRIAGE, recordId));
		assertAmendable(previous.getSupersededAt());
		requireReason(dto.amendmentReason());
		assertRegistryNumberFree(marriageRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo())
				.stream().anyMatch(r -> !r.getRecordId().equals(previous.getRecordId())));
		previous.setSupersededAt(LocalDateTime.now());
		marriageRepository.save(previous);
		MarriageRecord amended = toMarriage(dto, UUID.randomUUID().toString(), previous.getRecordId(),
				dto.amendmentReason(), CurrentUser.username());
		save(amended);
		audit(T_MARRIAGE, AuditAction.UPDATE, json(K_RECORD_ID, amended.getRecordId()), requestId);
		return amended.getRecordId();
	}

	@Transactional(readOnly = true)
	public MarriageResponseDTO getMarriage(String recordId, String purpose, String requestId) {
		MarriageRecord record = marriageRepository.findById(recordId)
				.orElseThrow(() -> notFound(LABEL_MARRIAGE, recordId));
		assertOfficeAllowed(record.getOfficeCode());
		// A marriage read touches both spouses' personal data — log access for each subject.
		logAccess(record.getSpouse1Uid(), purpose, requestId);
		logAccess(record.getSpouse2Uid(), purpose, requestId);
		return toMarriageDto(record);
	}

	@Transactional
	public String addDissolution(String marriageId, DissolutionRequestDTO dto, String requestId) {
		MarriageRecord marriage = marriageRepository.findById(marriageId)
				.orElseThrow(() -> notFound(LABEL_MARRIAGE, marriageId));
		assertOfficeAllowed(marriage.getOfficeCode());
		MarriageDissolution dissolution = new MarriageDissolution();
		dissolution.setDissolutionId(UUID.randomUUID().toString());
		dissolution.setMarriageId(marriageId);
		dissolution.setDissolutionType(dto.dissolutionType());
		dissolution.setEffectiveDate(dto.effectiveDate());
		dissolution.setCourtCode(dto.courtCode());
		dissolution.setCourtReference(dto.courtReference());
		dissolution.setRecordedAt(LocalDateTime.now());
		dissolution.setRecordedBy(CurrentUser.username());
		dissolutionRepository.save(dissolution);
		audit(T_DISSOLUTION, AuditAction.INSERT, json("dissolutionId", dissolution.getDissolutionId()), requestId);
		logAccess(marriage.getSpouse1Uid(), "marriage-dissolution", requestId);
		logAccess(marriage.getSpouse2Uid(), "marriage-dissolution", requestId);
		return dissolution.getDissolutionId();
	}

	// ----------------------------------------------------------------- deaths

	@Transactional
	public String registerDeath(DeathRequestDTO dto, String requestId) {
		assertOfficeAllowed(dto.officeCode());
		assertRegistryNumberFree(!deathRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo()).isEmpty());
		DeathRecord record = toDeath(dto, UUID.randomUUID().toString(), null, null, null);
		save(record);
		audit(T_DEATH, AuditAction.INSERT, json(K_RECORD_ID, record.getRecordId()), requestId);
		logAccess(dto.personUid(), "death-registration", requestId);
		return record.getRecordId();
	}

	@Transactional
	public String amendDeath(String recordId, DeathRequestDTO dto, String requestId) {
		DeathRecord previous = deathRepository.findById(recordId)
				.orElseThrow(() -> notFound(LABEL_DEATH, recordId));
		assertAmendable(previous.getSupersededAt());
		requireReason(dto.amendmentReason());
		assertRegistryNumberFree(deathRepository
				.findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
						dto.officeCode(), dto.bookYear(), dto.entryNo())
				.stream().anyMatch(r -> !r.getRecordId().equals(previous.getRecordId())));
		previous.setSupersededAt(LocalDateTime.now());
		deathRepository.save(previous);
		DeathRecord amended = toDeath(dto, UUID.randomUUID().toString(), previous.getRecordId(),
				dto.amendmentReason(), CurrentUser.username());
		save(amended);
		audit(T_DEATH, AuditAction.UPDATE, json(K_RECORD_ID, amended.getRecordId()), requestId);
		return amended.getRecordId();
	}

	@Transactional(readOnly = true)
	public DeathResponseDTO getDeath(String recordId, String purpose, String requestId) {
		DeathRecord record = deathRepository.findById(recordId)
				.orElseThrow(() -> notFound(LABEL_DEATH, recordId));
		assertOfficeAllowed(record.getOfficeCode());
		logAccess(record.getPersonUid(), purpose, requestId);
		return toDeathDto(record);
	}

	/** Cause of death (Art. 9). Clerk/supervisor only; upsert on the death record id. */
	@Transactional
	public void setMedical(String deathRecordId, MedicalRequestDTO dto, String requestId) {
		DeathRecord death = deathRepository.findById(deathRecordId)
				.orElseThrow(() -> notFound(LABEL_DEATH, deathRecordId));
		assertOfficeAllowed(death.getOfficeCode());
		Optional<DeathMedical> existing = deathMedicalRepository.findById(deathRecordId);
		DeathMedical medical = existing.orElseGet(DeathMedical::new);
		medical.setDeathRecordId(deathRecordId);
		medical.setCauseOfDeathIcd10(dto.causeOfDeathIcd10());
		medical.setAutopsyPerformed(dto.autopsyPerformed());
		medical.setCertifiedBy(dto.certifiedBy());
		medical.setRecordedAt(LocalDateTime.now());
		medical.setRecordedBy(CurrentUser.username());
		deathMedicalRepository.save(medical);
		audit(T_MEDICAL, existing.isPresent() ? AuditAction.UPDATE : AuditAction.INSERT,
				json("deathRecordId", deathRecordId), requestId);
	}

	@Transactional(readOnly = true)
	public MedicalResponseDTO getMedical(String deathRecordId, String purpose, String requestId) {
		DeathRecord death = deathRepository.findById(deathRecordId)
				.orElseThrow(() -> notFound(LABEL_DEATH, deathRecordId));
		assertOfficeAllowed(death.getOfficeCode());
		DeathMedical medical = deathMedicalRepository.findById(deathRecordId)
				.orElseThrow(() -> notFound("Death medical", deathRecordId));
		logAccess(death.getPersonUid(), purpose, requestId);
		return new MedicalResponseDTO(medical.getDeathRecordId(), medical.getCauseOfDeathIcd10(),
				medical.getAutopsyPerformed(), medical.getCertifiedBy(), medical.getRecordedAt());
	}

	// -------------------------------------------------- Art. 15 export slices
	// Vital records where the person is a subject, assembled for the GDPR export. No access log —
	// the export operation records a single disclosure + access-log entry in GdprApiService.

	@Transactional(readOnly = true)
	public List<BirthResponseDTO> birthsForSubject(String personUid) {
		return birthRepository.findByChildUid(personUid).stream()
				.map(VitalRecordsApiService::toBirthDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MarriageResponseDTO> marriagesForSubject(String personUid) {
		return marriageRepository.findBySpouse1UidOrSpouse2Uid(personUid, personUid).stream()
				.map(VitalRecordsApiService::toMarriageDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DeathResponseDTO> deathsForSubject(String personUid) {
		return deathRepository.findByPersonUid(personUid).stream()
				.map(VitalRecordsApiService::toDeathDto)
				.toList();
	}

	// ---------------------------------------------------------------- internals

	private void assertOfficeAllowed(String officeCode) {
		if (CLERK_AUTHORITY.equals(CurrentUser.role())) {
			String own = CurrentUser.officeCode();
			if (own == null || !own.equals(officeCode)) {
				throw new AccessDeniedException("Clerk is scoped to office " + own);
			}
		}
	}

	private static void assertRegistryNumberFree(boolean taken) {
		if (taken) {
			throw new ConflictException("Registry number already used for this office, year and entry");
		}
	}

	private static void requireReason(String reason) {
		if (!StringUtils.hasText(reason)) {
			throw new ConflictException("An amendment requires a reason");
		}
	}

	/** Only the current (non-superseded) version of a record may be amended — no forked chains. */
	private static void assertAmendable(LocalDateTime supersededAt) {
		if (supersededAt != null) {
			throw new ConflictException("Cannot amend a superseded record version");
		}
	}

	private void logAccess(String personUid, String purpose, String requestId) {
		accessLogService.record(CurrentUser.username(), personUid, purpose, null, "vital-record", requestId);
	}

	private void audit(String table, AuditAction action, String newRow, String requestId) {
		auditService.record(SCHEMA, table, action, null, newRow, CurrentUser.username(), CurrentUser.role(),
				requestId, null);
	}

	private void save(BirthRecord record) {
		record.setRecordedAt(LocalDateTime.now());
		record.setRecordedBy(CurrentUser.username());
		birthRepository.save(record);
	}

	private void save(MarriageRecord record) {
		record.setRecordedAt(LocalDateTime.now());
		record.setRecordedBy(CurrentUser.username());
		marriageRepository.save(record);
	}

	private void save(DeathRecord record) {
		record.setRecordedAt(LocalDateTime.now());
		record.setRecordedBy(CurrentUser.username());
		deathRepository.save(record);
	}

	private static EntityNotFoundException notFound(String what, String id) {
		return new EntityNotFoundException(what + " not found: " + id);
	}

	private static String json(String key, String value) {
		return "{\"" + key + "\":\"" + value + "\"}";
	}

	private static BirthRecord toBirth(BirthRequestDTO dto, String recordId, String supersedesId,
			String amendmentReason, String approvedBy) {
		BirthRecord r = new BirthRecord();
		r.setRecordId(recordId);
		r.setOfficeCode(dto.officeCode());
		r.setBookYear(dto.bookYear());
		r.setEntryNo(dto.entryNo());
		r.setChildUid(dto.childUid());
		r.setDateOfBirth(dto.dateOfBirth());
		r.setTimeOfBirth(dto.timeOfBirth());
		r.setPlaceMunicipality(dto.placeMunicipality());
		r.setPlaceForeign(dto.placeForeign());
		r.setFacility(dto.facility());
		r.setMultipleBirthOrder(dto.multipleBirthOrder());
		r.setMotherUid(dto.motherUid());
		r.setFatherUid(dto.fatherUid());
		r.setSupersedesId(supersedesId);
		r.setAmendmentReason(amendmentReason);
		r.setApprovedBy(approvedBy);
		return r;
	}

	private static MarriageRecord toMarriage(MarriageRequestDTO dto, String recordId, String supersedesId,
			String amendmentReason, String approvedBy) {
		MarriageRecord r = new MarriageRecord();
		r.setRecordId(recordId);
		r.setOfficeCode(dto.officeCode());
		r.setBookYear(dto.bookYear());
		r.setEntryNo(dto.entryNo());
		r.setSpouse1Uid(dto.spouse1Uid());
		r.setSpouse2Uid(dto.spouse2Uid());
		r.setMarriageDate(dto.marriageDate());
		r.setPlaceMunicipality(dto.placeMunicipality());
		r.setPlaceForeign(dto.placeForeign());
		r.setMarriageForm(dto.marriageForm());
		r.setOfficiant(dto.officiant());
		r.setWitness1Name(dto.witness1Name());
		r.setWitness2Name(dto.witness2Name());
		r.setSurnameAgreement(dto.surnameAgreement());
		r.setSupersedesId(supersedesId);
		r.setAmendmentReason(amendmentReason);
		r.setApprovedBy(approvedBy);
		return r;
	}

	private static DeathRecord toDeath(DeathRequestDTO dto, String recordId, String supersedesId,
			String amendmentReason, String approvedBy) {
		DeathRecord r = new DeathRecord();
		r.setRecordId(recordId);
		r.setOfficeCode(dto.officeCode());
		r.setBookYear(dto.bookYear());
		r.setEntryNo(dto.entryNo());
		r.setPersonUid(dto.personUid());
		r.setDateOfDeath(dto.dateOfDeath());
		r.setDateFound(dto.dateFound());
		r.setDeclaredByCourt(dto.declaredByCourt());
		r.setCourtCode(dto.courtCode());
		r.setCourtReference(dto.courtReference());
		r.setPlaceMunicipality(dto.placeMunicipality());
		r.setPlaceForeign(dto.placeForeign());
		r.setSupersedesId(supersedesId);
		r.setAmendmentReason(amendmentReason);
		r.setApprovedBy(approvedBy);
		return r;
	}

	private static BirthResponseDTO toBirthDto(BirthRecord r) {
		return new BirthResponseDTO(r.getRecordId(), r.getOfficeCode(), r.getBookYear(), r.getEntryNo(),
				r.getChildUid(), r.getDateOfBirth(), r.getTimeOfBirth(), r.getPlaceMunicipality(),
				r.getPlaceForeign(), r.getFacility(), r.getMultipleBirthOrder(), r.getMotherUid(),
				r.getFatherUid(), r.getRecordedAt(), r.getSupersededAt(), r.getSupersedesId(),
				r.getAmendmentReason(), r.getApprovedBy());
	}

	private static MarriageResponseDTO toMarriageDto(MarriageRecord r) {
		return new MarriageResponseDTO(r.getRecordId(), r.getOfficeCode(), r.getBookYear(), r.getEntryNo(),
				r.getSpouse1Uid(), r.getSpouse2Uid(), r.getMarriageDate(), r.getPlaceMunicipality(),
				r.getPlaceForeign(), r.getMarriageForm(), r.getOfficiant(), r.getWitness1Name(),
				r.getWitness2Name(), r.getSurnameAgreement(), r.getRecordedAt(), r.getSupersededAt(),
				r.getSupersedesId(), r.getAmendmentReason(), r.getApprovedBy());
	}

	private static DeathResponseDTO toDeathDto(DeathRecord r) {
		return new DeathResponseDTO(r.getRecordId(), r.getOfficeCode(), r.getBookYear(), r.getEntryNo(),
				r.getPersonUid(), r.getDateOfDeath(), r.getDateFound(), r.isDeclaredByCourt(), r.getCourtCode(),
				r.getCourtReference(), r.getPlaceMunicipality(), r.getPlaceForeign(), r.getRecordedAt(),
				r.getSupersededAt(), r.getSupersedesId(), r.getAmendmentReason(), r.getApprovedBy());
	}
}
