package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A bitemporal religion version (Art. 9). */
public record ReligionResponseDTO(
		Long personReligionId,
		String religionCode,
		String legalBasis,
		Long consentId,
		LocalDate validFrom,
		LocalDate validTo,
		LocalDateTime supersededAt) {
}
