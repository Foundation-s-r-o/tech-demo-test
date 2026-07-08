package sk.foundation.techdemo.gdpr.api;

import java.time.LocalDateTime;

/** A consent record. */
public record ConsentResponseDTO(
		Long consentId,
		String personUid,
		String purpose,
		LocalDateTime givenAt,
		String givenVia,
		LocalDateTime withdrawnAt) {
}
