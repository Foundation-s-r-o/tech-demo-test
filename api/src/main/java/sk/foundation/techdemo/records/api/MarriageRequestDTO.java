package sk.foundation.techdemo.records.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Register or amend a marriage record. {@code amendmentReason} is required only for amendments. */
public record MarriageRequestDTO(
		@NotBlank String officeCode,
		@NotNull Integer bookYear,
		@NotNull Integer entryNo,
		@NotBlank String spouse1Uid,
		@NotBlank String spouse2Uid,
		@NotNull LocalDate marriageDate,
		String placeMunicipality,
		String placeForeign,
		@NotBlank String marriageForm,
		String officiant,
		String witness1Name,
		String witness2Name,
		String surnameAgreement,
		String amendmentReason) {
}
