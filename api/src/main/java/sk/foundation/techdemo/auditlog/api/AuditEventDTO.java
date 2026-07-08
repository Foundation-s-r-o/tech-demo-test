package sk.foundation.techdemo.auditlog.api;

import java.time.LocalDateTime;

/** A row-change audit entry (without the JSON payloads). */
public record AuditEventDTO(
		Long auditId,
		LocalDateTime eventTime,
		String actor,
		String dbRole,
		String action,
		String schemaName,
		String tableName,
		String requestId,
		String legalBasis,
		String rowHash) {
}
