package sk.foundation.techdemo.records.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Record a marriage dissolution (divorce, annulment, or spouse death). */
public record DissolutionRequestDTO(
		@NotBlank String dissolutionType,
		@NotNull LocalDate effectiveDate,
		String courtCode,
		String courtReference) {
}
