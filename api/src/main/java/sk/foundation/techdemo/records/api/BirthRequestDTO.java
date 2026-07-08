package sk.foundation.techdemo.records.api;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Register or amend a birth record. {@code amendmentReason} is required only for amendments. */
public record BirthRequestDTO(
		@NotBlank String officeCode,
		@NotNull Integer bookYear,
		@NotNull Integer entryNo,
		@NotBlank String childUid,
		@NotNull LocalDate dateOfBirth,
		LocalTime timeOfBirth,
		String placeMunicipality,
		String placeForeign,
		String facility,
		Integer multipleBirthOrder,
		String motherUid,
		String fatherUid,
		String amendmentReason) {
}
