package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A bitemporal address version. */
public record AddressResponseDTO(
		Long personAddressId,
		String addressType,
		String street,
		String houseNumber,
		String postalCode,
		String municipalityCode,
		String municipalityPartCode,
		String countryCode,
		String foreignAddress,
		LocalDate validFrom,
		LocalDate validTo,
		LocalDateTime supersededAt) {
}
