package sk.foundation.techdemo.gdpr.api;

import java.time.LocalDateTime;

/** A disclosure-register entry. */
public record DisclosureResponseDTO(
		Long disclosureId,
		String personUid,
		String recipient,
		String recipientType,
		String legalBasis,
		String disclosedFields,
		LocalDateTime disclosedAt,
		String disclosedBy,
		Long dsrRequestId) {
}
