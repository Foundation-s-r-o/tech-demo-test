package sk.foundation.techdemo.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import sk.foundation.techdemo.BaseIT;
import sk.foundation.techdemo.auditlog.AuditAction;
import sk.foundation.techdemo.auditlog.AuditLog;
import sk.foundation.techdemo.auditlog.AuditLogRepository;
import sk.foundation.techdemo.auditlog.AuditService;
import sk.foundation.techdemo.auditlog.ChainVerificationResult;
import sk.foundation.techdemo.infrastructure.api.ConflictException;
import sk.foundation.techdemo.infrastructure.crypto.NationalIdCryptoService;

/**
 * Exercises the registry domain services against the real embedded H2 schema (profile {@code it}):
 * the audit hash chain and tamper detection (test_schema.sql T7), bitemporal overlap rejection
 * against persisted rows (T3), person/detail persistence with real national-id crypto, and the
 * identity-merge guard + resolution. Each test rolls back ({@code @Transactional}).
 *
 * <p>Uses a dedicated in-memory database (own context) so this write-heavy suite is isolated
 * from the shared {@code it} database that other ITs churn via {@code @DirtiesContext}.
 */
@Transactional
@TestPropertySource(properties =
		"spring.datasource.url=jdbc:h2:mem:registrydomainit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class RegistryDomainIT extends BaseIT {

	@Autowired
	private RegistryPersonRepository personRepository;
	@Autowired
	private PersonDetailRepository personDetailRepository;
	@Autowired
	private PersonUidService personUidService;
	@Autowired
	private BitemporalGuard bitemporalGuard;
	@Autowired
	private PersonMergeService personMergeService;
	@Autowired
	private NationalIdCryptoService cryptoService;
	@Autowired
	private AuditService auditService;
	@Autowired
	private AuditLogRepository auditLogRepository;

	@Test
	void auditChainVerifiesAndDetectsTampering() {
		auditService.record("REGISTRY", "PERSON", AuditAction.INSERT, null, "{\"a\":1}",
				"admin", "SUPERVISOR", "req-1", "art6_1c");
		auditService.record("REGISTRY", "PERSON_DETAIL", AuditAction.INSERT, null, "{\"b\":2}",
				"admin", "SUPERVISOR", "req-2", "art6_1c");

		assertTrue(auditService.verifyChain().intact(), "freshly built chain must verify");

		List<AuditLog> entries = auditLogRepository.findAllByOrderByAuditIdAsc();
		AuditLog last = entries.get(entries.size() - 1);
		last.setNewRow("{\"b\":2,\"tampered\":true}");
		auditLogRepository.saveAndFlush(last);

		ChainVerificationResult result = auditService.verifyChain();
		assertFalse(result.intact(), "editing a stored row must break the chain");
		assertEquals(last.getAuditId(), result.firstBrokenId());
	}

	@Test
	void deletingNewestAuditRowBreaksChainTail() {
		auditService.record("REGISTRY", "PERSON", AuditAction.INSERT, null, "{\"a\":1}",
				"admin", "SUPERVISOR", "req-1", "art6_1c");
		auditService.record("REGISTRY", "PERSON", AuditAction.INSERT, null, "{\"b\":2}",
				"admin", "SUPERVISOR", "req-2", "art6_1c");
		assertTrue(auditService.verifyChain().intact(), "freshly built chain must verify");

		List<AuditLog> entries = auditLogRepository.findAllByOrderByAuditIdAsc();
		AuditLog last = entries.get(entries.size() - 1);
		auditLogRepository.delete(last);
		auditLogRepository.flush();

		// The per-row loop can no longer see the deleted row; only the chain-head tail check
		// catches that the head still points at the removed row's hash.
		assertFalse(auditService.verifyChain().intact(),
				"deleting the newest audit row must break the chain tail");
	}

	@Test
	void bitemporalDetailOverlapIsRejectedAgainstPersistedRows() {
		String uid = persistPerson("8801011234");
		persistDetail(uid, LocalDate.parse("2000-01-01"), null);

		assertThrows(ConflictException.class,
				() -> bitemporalGuard.assertDetailInsertable(uid, LocalDate.parse("2010-01-01"), null));
		assertDoesNotThrow(
				() -> bitemporalGuard.assertDetailInsertable(uid,
						LocalDate.parse("1990-01-01"), LocalDate.parse("1995-01-01")));
	}

	@Test
	void nationalIdIsStoredEncryptedWithLookupHmac() {
		String uid = persistPerson("9002022345");
		var person = personRepository.findById(uid).orElseThrow();
		assertEquals("9002022345", cryptoService.decrypt(person.getNationalIdEnc()));
		assertEquals(cryptoService.hmac("9002022345"), person.getNationalIdHmac());
	}

	@Test
	void mergeGuardAndResolution() {
		String duplicate = persistPerson(null);
		String primary = persistPerson(null);

		personMergeService.merge(duplicate, primary, "same physical person", "admin", "admin");
		assertEquals(primary, personMergeService.resolvePersonUid(duplicate));
		assertEquals(primary, personMergeService.resolvePersonUid(primary));
		assertThrows(ConflictException.class,
				() -> personMergeService.merge(duplicate, primary, "again", "admin", "admin"));
	}

	private String persistPerson(String nationalId) {
		Person person = new Person();
		person.setPersonUid(personUidService.generate());
		if (nationalId != null) {
			person.setNationalIdEnc(cryptoService.encrypt(nationalId));
			person.setNationalIdHmac(cryptoService.hmac(nationalId));
		}
		person.setAnonymized(false);
		person.setCreatedAt(LocalDateTime.now());
		person.setCreatedBy("admin");
		personRepository.saveAndFlush(person);
		return person.getPersonUid();
	}

	private void persistDetail(String personUid, LocalDate validFrom, LocalDate validTo) {
		PersonDetail detail = new PersonDetail();
		detail.setPersonUid(personUid);
		detail.setGivenNames("Jan");
		detail.setFamilyName("Novak");
		detail.setSex("M");
		detail.setDateOfBirth(LocalDate.parse("1980-01-01"));
		detail.setDateOfBirthPrecision("day");
		detail.setCitizenship("703");
		detail.setValidFrom(validFrom);
		detail.setValidTo(validTo);
		detail.setRecordedAt(LocalDateTime.now());
		detail.setRecordedBy("admin");
		personDetailRepository.saveAndFlush(detail);
	}
}
