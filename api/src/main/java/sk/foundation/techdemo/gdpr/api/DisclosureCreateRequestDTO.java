package sk.foundation.techdemo.gdpr.api;

import jakarta.validation.constraints.NotBlank;

/** Record a disclosure of personal data (Art. 30). Fields are a CSV list. */
public record DisclosureCreateRequestDTO(
		@NotBlank String personUid,
		@NotBlank String recipient,
		@NotBlank String recipientType,
		@NotBlank String legalBasis,
		@NotBlank String disclosedFields) {
}
