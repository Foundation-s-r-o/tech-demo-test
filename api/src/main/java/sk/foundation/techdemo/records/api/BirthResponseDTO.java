package sk.foundation.techdemo.records.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** A birth record version. */
public record BirthResponseDTO(
		String recordId,
		String officeCode,
		Integer bookYear,
		Integer entryNo,
		String childUid,
		LocalDate dateOfBirth,
		LocalTime timeOfBirth,
		String placeMunicipality,
		String placeForeign,
		String facility,
		Integer multipleBirthOrder,
		String motherUid,
		String fatherUid,
		LocalDateTime recordedAt,
		LocalDateTime supersededAt,
		String supersedesId,
		String amendmentReason,
		String approvedBy) {
}
