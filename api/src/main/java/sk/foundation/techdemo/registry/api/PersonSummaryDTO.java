package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

/** Minimal person summary for search results. */
public record PersonSummaryDTO(
		String uid,
		String givenNames,
		String familyName,
		LocalDate dateOfBirth,
		String sex) {
}
