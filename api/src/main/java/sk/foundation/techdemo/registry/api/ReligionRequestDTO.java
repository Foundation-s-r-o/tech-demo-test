package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * New religion version (GDPR Art. 9). When {@code legalBasis} is {@code art9_2a_consent}, a
 * consent is recorded from {@code consentPurpose}/{@code consentGivenVia} and linked.
 */
public record ReligionRequestDTO(
		@NotBlank String religionCode,
		@NotBlank String legalBasis,
		@NotNull LocalDate validFrom,
		LocalDate validTo,
		String changeReason,
		String consentPurpose,
		String consentGivenVia) {
}
