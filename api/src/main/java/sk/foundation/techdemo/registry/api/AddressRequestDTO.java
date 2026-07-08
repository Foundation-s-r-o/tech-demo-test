package sk.foundation.techdemo.registry.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** New bitemporal address version of a given type; supersedes the current one of that type. */
public record AddressRequestDTO(
		String street,
		String houseNumber,
		String postalCode,
		String municipalityCode,
		String municipalityPartCode,
		@NotBlank String countryCode,
		String foreignAddress,
		@NotBlank String addressType,
		@NotNull LocalDate validFrom,
		LocalDate validTo,
		String changeReason) {
}
