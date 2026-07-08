package sk.foundation.techdemo.records.api;

import java.time.LocalDateTime;

/** Cause-of-death (Art. 9 health data). */
public record MedicalResponseDTO(
		String deathRecordId,
		String causeOfDeathIcd10,
		Boolean autopsyPerformed,
		String certifiedBy,
		LocalDateTime recordedAt) {
}
