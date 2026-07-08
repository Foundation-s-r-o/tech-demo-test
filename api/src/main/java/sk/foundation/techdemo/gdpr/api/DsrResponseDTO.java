package sk.foundation.techdemo.gdpr.api;

import java.time.LocalDate;

/** A data subject request. */
public record DsrResponseDTO(
		Long requestId,
		String personUid,
		String requesterRef,
		String requestType,
		LocalDate receivedAt,
		LocalDate dueAt,
		String status,
		String handledBy,
		LocalDate resolvedAt,
		String resolution) {
}
