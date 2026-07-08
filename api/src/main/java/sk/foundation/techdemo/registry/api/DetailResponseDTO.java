package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A bitemporal identity-detail version. */
public record DetailResponseDTO(
		Long detailId,
		String givenNames,
		String familyName,
		String birthSurname,
		String sex,
		LocalDate dateOfBirth,
		String dateOfBirthPrecision,
		String placeOfBirthMunicipality,
		String placeOfBirthForeign,
		String citizenship,
		String maritalStatusCode,
		String existenceStatusCode,
		String titlesBefore,
		String titlesAfter,
		LocalDate validFrom,
		LocalDate validTo,
		LocalDateTime recordedAt,
		LocalDateTime supersededAt) {
}
