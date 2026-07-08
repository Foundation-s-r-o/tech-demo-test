package sk.foundation.techdemo.records.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A marriage dissolution. */
public record DissolutionResponseDTO(
		String dissolutionId,
		String marriageId,
		String dissolutionType,
		LocalDate effectiveDate,
		String courtCode,
		String courtReference,
		LocalDateTime recordedAt) {
}
