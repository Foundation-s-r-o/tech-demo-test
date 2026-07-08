package sk.foundation.techdemo.records.api;

/** Cause-of-death (Art. 9 health data) attached to a death record. */
public record MedicalRequestDTO(
		String causeOfDeathIcd10,
		Boolean autopsyPerformed,
		String certifiedBy) {
}
