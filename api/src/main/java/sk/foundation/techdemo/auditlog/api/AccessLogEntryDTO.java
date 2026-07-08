package sk.foundation.techdemo.auditlog.api;

import java.time.LocalDateTime;

/** A read-access log entry (GDPR Art. 15). */
public record AccessLogEntryDTO(
		Long accessId,
		LocalDateTime accessedAt,
		String actor,
		String personUid,
		String purpose,
		String legalBasis,
		String fields,
		String requestId) {
}
