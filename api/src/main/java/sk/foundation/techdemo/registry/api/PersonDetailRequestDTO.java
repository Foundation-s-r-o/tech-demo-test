package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** New bitemporal identity-detail version; supersedes the current one. */
public record PersonDetailRequestDTO(
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
		String existenceStatusCode,
		String titlesBefore,
		String titlesAfter,
		@NotNull LocalDate validFrom,
		LocalDate validTo,
		String changeReason) {
}
