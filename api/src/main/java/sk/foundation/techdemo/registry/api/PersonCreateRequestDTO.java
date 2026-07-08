package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create a person and their initial identity detail. The national id (optional) is stored only
 * encrypted + HMAC'd; {@code validFrom} defaults to the date of birth when omitted.
 */
public record PersonCreateRequestDTO(
		String nationalId,
		@NotBlank String givenNames,
		@NotBlank String familyName,
		String birthSurname,
		@NotBlank String sex,
		@NotNull LocalDate dateOfBirth,
		String dateOfBirthPrecision,
		String placeOfBirthMunicipality,
		String placeOfBirthForeign,
		@NotBlank String citizenship,
		String maritalStatusCode,
		String titlesBefore,
		String titlesAfter,
		LocalDate validFrom) {
}
