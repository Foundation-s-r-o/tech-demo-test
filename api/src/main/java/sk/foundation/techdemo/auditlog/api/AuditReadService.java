package sk.foundation.techdemo.auditlog.api;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.AccessLogService;
import sk.foundation.techdemo.auditlog.AuditLogRepository;
import sk.foundation.techdemo.auditlog.AuditService;
import sk.foundation.techdemo.auditlog.ChainVerificationResult;

/** Read side of the audit trail: row-change events, read-access log, and chain verification. */
@Service
@RequiredArgsConstructor
public class AuditReadService {

	private final AuditLogRepository auditLogRepository;
	private final AccessLogService accessLogService;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public List<AuditEventDTO> events() {
		return auditLogRepository.findAllByOrderByAuditIdAsc().stream()
				.map(e -> new AuditEventDTO(e.getAuditId(), e.getEventTime(), e.getActor(), e.getDbRole(),
						e.getAction(), e.getSchemaName(), e.getTableName(), e.getRequestId(), e.getLegalBasis(),
						e.getRowHash()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AccessLogEntryDTO> accessLog(String personUid) {
		return accessLogService.forPerson(personUid).stream()
				.map(a -> new AccessLogEntryDTO(a.getAccessId(), a.getAccessedAt(), a.getActor(), a.getPersonUid(),
						a.getPurpose(), a.getLegalBasis(), a.getFields(), a.getRequestId()))
				.toList();
	}

	public ChainVerificationResult verifyChain() {
		return auditService.verifyChain();
	}
}
