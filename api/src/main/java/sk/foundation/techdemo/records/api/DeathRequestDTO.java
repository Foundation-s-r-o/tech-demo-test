package sk.foundation.techdemo.records.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Register or amend a death record. {@code amendmentReason} is required only for amendments. */
public record DeathRequestDTO(
		@NotBlank String officeCode,
		@NotNull Integer bookYear,
		@NotNull Integer entryNo,
		@NotBlank String personUid,
		LocalDate dateOfDeath,
		LocalDate dateFound,
		boolean declaredByCourt,
		String courtCode,
		String courtReference,
		String placeMunicipality,
		String placeForeign,
		String amendmentReason) {
}
