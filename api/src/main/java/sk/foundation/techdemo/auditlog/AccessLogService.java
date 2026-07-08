package sk.foundation.techdemo.auditlog;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Records read access to personal data (GDPR Art. 15 "who saw my data") into AUDIT.ACCESS_LOG,
 * with purpose, legal basis, accessed fields (CSV), and request correlation.
 */
@Service
@RequiredArgsConstructor
public class AccessLogService {

	private final AccessLogRepository accessLogRepository;

	@Transactional
	public void record(String actor, String personUid, String purpose, String legalBasis,
			String fields, String requestId) {
		AccessLog entry = new AccessLog();
		entry.setAccessedAt(LocalDateTime.now());
		entry.setActor(actor);
		entry.setPersonUid(personUid);
		entry.setPurpose(purpose);
		entry.setLegalBasis(legalBasis);
		entry.setFields(fields);
		entry.setRequestId(requestId);
		accessLogRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public List<AccessLog> forPerson(String personUid) {
		return accessLogRepository.findByPersonUidOrderByAccessedAtDesc(personUid);
	}
}
