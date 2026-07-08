package sk.foundation.techdemo.gdpr.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.AccessLogService;
import sk.foundation.techdemo.auditlog.AuditAction;
import sk.foundation.techdemo.auditlog.AuditService;
import sk.foundation.techdemo.auditlog.api.AccessLogEntryDTO;
import sk.foundation.techdemo.auth.CurrentUser;
import sk.foundation.techdemo.gdpr.Consent;
import sk.foundation.techdemo.gdpr.ConsentRepository;
import sk.foundation.techdemo.gdpr.DataSubjectRequest;
import sk.foundation.techdemo.gdpr.DataSubjectRequestRepository;
import sk.foundation.techdemo.gdpr.DisclosureRegister;
import sk.foundation.techdemo.gdpr.DisclosureRegisterRepository;
import sk.foundation.techdemo.records.api.VitalRecordsApiService;
import sk.foundation.techdemo.registry.PersonReligion;
import sk.foundation.techdemo.registry.PersonReligionRepository;
import sk.foundation.techdemo.registry.api.PersonCompositeResponseDTO;
import sk.foundation.techdemo.registry.api.RegistryPersonApiService;

/**
 * GDPR operations: data subject requests (statutory one-month due date), consents with withdrawal
 * (which supersedes the religion rows it backed), the disclosure register, and the Art. 15 export
 * (itself recorded as a disclosure). Writes append to the audit log.
 */
@Service
@RequiredArgsConstructor
public class GdprApiService {

	private static final String SCHEMA = "GDPR";
	private static final String T_DSR = "DATA_SUBJECT_REQUEST";
	private static final String T_CONSENT = "CONSENT";
	private static final String T_DISCLOSURE = "DISCLOSURE_REGISTER";
	private static final String STATUS_RECEIVED = "received";
	private static final String DATA_SUBJECT = "data_subject";

	private final DataSubjectRequestRepository dsrRepository;
	private final ConsentRepository consentRepository;
	private final DisclosureRegisterRepository disclosureRepository;
	private final PersonReligionRepository religionRepository;
	private final RegistryPersonApiService personApiService;
	private final VitalRecordsApiService vitalRecordsApiService;
	private final AccessLogService accessLogService;
	private final AuditService auditService;

	// ------------------------------------------------------------------- DSR

	@Transactional
	public Long createDsr(DsrCreateRequestDTO dto, String requestId) {
		DataSubjectRequest dsr = new DataSubjectRequest();
		dsr.setPersonUid(dto.personUid());
		dsr.setRequesterRef(dto.requesterRef());
		dsr.setRequestType(dto.requestType());
		LocalDate today = LocalDate.now();
		dsr.setReceivedAt(today);
		dsr.setDueAt(today.plusMonths(1));
		dsr.setStatus(STATUS_RECEIVED);
		Long id = dsrRepository.save(dsr).getRequestId();
		audit(T_DSR, AuditAction.INSERT, requestId);
		return id;
	}

	@Transactional(readOnly = true)
	public List<DsrResponseDTO> listDsr(String personUid) {
		return dsrRepository.findByPersonUid(personUid).stream().map(GdprApiService::toDsr).toList();
	}

	@Transactional
	public DsrResponseDTO updateDsr(Long id, DsrUpdateRequestDTO dto, String requestId) {
		DataSubjectRequest dsr = dsrRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("DSR not found: " + id));
		dsr.setStatus(dto.status());
		dsr.setResolution(dto.resolution());
		dsr.setHandledBy(CurrentUser.username());
		if ("fulfilled".equals(dto.status()) || "refused".equals(dto.status())) {
			dsr.setResolvedAt(LocalDate.now());
		}
		dsrRepository.save(dsr);
		audit(T_DSR, AuditAction.UPDATE, requestId);
		return toDsr(dsr);
	}

	// --------------------------------------------------------------- consents

	@Transactional
	public Long createConsent(ConsentCreateRequestDTO dto, String requestId) {
		Consent consent = new Consent();
		consent.setPersonUid(dto.personUid());
		consent.setPurpose(dto.purpose());
		consent.setGivenAt(LocalDateTime.now());
		consent.setGivenVia(dto.givenVia());
		consent.setRecordedBy(CurrentUser.username());
		Long id = consentRepository.save(consent).getConsentId();
		audit(T_CONSENT, AuditAction.INSERT, requestId);
		return id;
	}

	@Transactional(readOnly = true)
	public List<ConsentResponseDTO> listConsent(String personUid) {
		return consentRepository.findByPersonUid(personUid).stream().map(GdprApiService::toConsent).toList();
	}

	/** Withdraw a consent (Art. 7) and supersede the religion rows it backed. */
	@Transactional
	public void withdrawConsent(Long id, String requestId) {
		Consent consent = consentRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Consent not found: " + id));
		LocalDateTime now = LocalDateTime.now();
		consent.setWithdrawnAt(now);
		consentRepository.save(consent);
		List<PersonReligion> backed = religionRepository.findByConsentIdAndSupersededAtIsNull(id);
		for (PersonReligion religion : backed) {
			religion.setSupersededAt(now);
			religion.setChangeReason("consent withdrawn");
		}
		religionRepository.saveAll(backed);
		audit(T_CONSENT, AuditAction.UPDATE, requestId);
	}

	// ------------------------------------------------------------ disclosures

	@Transactional
	public Long createDisclosure(DisclosureCreateRequestDTO dto, String requestId) {
		Long id = recordDisclosure(dto.personUid(), dto.recipient(), dto.recipientType(), dto.legalBasis(),
				dto.disclosedFields(), null);
		audit(T_DISCLOSURE, AuditAction.INSERT, requestId);
		return id;
	}

	@Transactional(readOnly = true)
	public List<DisclosureResponseDTO> listDisclosure(String personUid) {
		return disclosureRepository.findByPersonUid(personUid).stream()
				.map(GdprApiService::toDisclosure).toList();
	}

	// -------------------------------------------------------- Art. 15 export

	@Transactional
	public ExportResponseDTO export(String uid, String requestId) {
		// getComposite records the single Art. 15 access-log entry for this export; the slice
		// assemblers below deliberately do not log again. accessLog(uid) is read afterwards so the
		// export honestly includes the read it just created.
		PersonCompositeResponseDTO person = personApiService.getComposite(uid, "art15-export", requestId);
		ExportResponseDTO export = new ExportResponseDTO(uid, person,
				personApiService.detailHistoryForExport(uid),
				personApiService.addressHistoryForExport(uid),
				personApiService.religionHistoryForExport(uid),
				vitalRecordsApiService.birthsForSubject(uid),
				vitalRecordsApiService.marriagesForSubject(uid),
				vitalRecordsApiService.deathsForSubject(uid),
				listConsent(uid),
				listDisclosure(uid),
				accessLog(uid));
		recordDisclosure(uid, "data subject", DATA_SUBJECT, "art15", "full-export", null);
		audit(T_DISCLOSURE, AuditAction.INSERT, requestId);
		return export;
	}

	private List<AccessLogEntryDTO> accessLog(String uid) {
		return accessLogService.forPerson(uid).stream()
				.map(a -> new AccessLogEntryDTO(a.getAccessId(), a.getAccessedAt(), a.getActor(),
						a.getPersonUid(), a.getPurpose(), a.getLegalBasis(), a.getFields(), a.getRequestId()))
				.toList();
	}

	// ---------------------------------------------------------------- internals

	private Long recordDisclosure(String personUid, String recipient, String recipientType,
			String legalBasis, String fields, Long dsrRequestId) {
		DisclosureRegister disclosure = new DisclosureRegister();
		disclosure.setPersonUid(personUid);
		disclosure.setRecipient(recipient);
		disclosure.setRecipientType(recipientType);
		disclosure.setLegalBasis(legalBasis);
		disclosure.setDisclosedFields(fields);
		disclosure.setDisclosedAt(LocalDateTime.now());
		disclosure.setDisclosedBy(CurrentUser.username());
		disclosure.setDsrRequestId(dsrRequestId);
		return disclosureRepository.save(disclosure).getDisclosureId();
	}

	private void audit(String table, AuditAction action, String requestId) {
		auditService.record(SCHEMA, table, action, null, null, CurrentUser.username(), CurrentUser.role(),
				requestId, null);
	}

	private static DsrResponseDTO toDsr(DataSubjectRequest d) {
		return new DsrResponseDTO(d.getRequestId(), d.getPersonUid(), d.getRequesterRef(), d.getRequestType(),
				d.getReceivedAt(), d.getDueAt(), d.getStatus(), d.getHandledBy(), d.getResolvedAt(),
				d.getResolution());
	}

	private static ConsentResponseDTO toConsent(Consent c) {
		return new ConsentResponseDTO(c.getConsentId(), c.getPersonUid(), c.getPurpose(), c.getGivenAt(),
				c.getGivenVia(), c.getWithdrawnAt());
	}

	private static DisclosureResponseDTO toDisclosure(DisclosureRegister d) {
		return new DisclosureResponseDTO(d.getDisclosureId(), d.getPersonUid(), d.getRecipient(),
				d.getRecipientType(), d.getLegalBasis(), d.getDisclosedFields(), d.getDisclosedAt(),
				d.getDisclosedBy(), d.getDsrRequestId());
	}
}
