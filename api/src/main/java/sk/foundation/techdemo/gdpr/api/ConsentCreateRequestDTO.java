package sk.foundation.techdemo.gdpr.api;

import jakarta.validation.constraints.NotBlank;

/** Record a consent. */
public record ConsentCreateRequestDTO(
		@NotBlank String personUid,
		@NotBlank String purpose,
		@NotBlank String givenVia) {
}
