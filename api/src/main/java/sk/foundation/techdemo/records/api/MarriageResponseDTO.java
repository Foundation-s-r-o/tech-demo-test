package sk.foundation.techdemo.records.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A marriage record version. */
public record MarriageResponseDTO(
		String recordId,
		String officeCode,
		Integer bookYear,
		Integer entryNo,
		String spouse1Uid,
		String spouse2Uid,
		LocalDate marriageDate,
		String placeMunicipality,
		String placeForeign,
		String marriageForm,
		String officiant,
		String witness1Name,
		String witness2Name,
		String surnameAgreement,
		LocalDateTime recordedAt,
		LocalDateTime supersededAt,
		String supersedesId,
		String amendmentReason,
		String approvedBy) {
}
