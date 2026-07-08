package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

/**
 * Minimal current-state projection for the AGENCY_READER role — the {@code V_PERSON_CURRENT}
 * column set: identity, date of birth, sex, existence status, and the current permanent address.
 * Deliberately excludes Art. 9 / health data, full history, and vital records.
 */
public record AgencyPersonDTO(
		String uid,
		String givenNames,
		String familyName,
		LocalDate dateOfBirth,
		String sex,
		String existenceStatusCode,
		AddressResponseDTO permanentAddress) {
}
