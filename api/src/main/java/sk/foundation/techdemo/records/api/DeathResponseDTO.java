package sk.foundation.techdemo.records.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A death record version (administrative; cause of death is isolated in medical). */
public record DeathResponseDTO(
		String recordId,
		String officeCode,
		Integer bookYear,
		Integer entryNo,
		String personUid,
		LocalDate dateOfDeath,
		LocalDate dateFound,
		boolean declaredByCourt,
		String courtCode,
		String courtReference,
		String placeMunicipality,
		String placeForeign,
		LocalDateTime recordedAt,
		LocalDateTime supersededAt,
		String supersedesId,
		String amendmentReason,
		String approvedBy) {
}
