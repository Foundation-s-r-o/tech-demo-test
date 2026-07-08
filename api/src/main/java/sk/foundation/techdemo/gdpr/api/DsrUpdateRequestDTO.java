package sk.foundation.techdemo.gdpr.api;

import jakarta.validation.constraints.NotBlank;

/** Advance a data subject request's status and record its resolution. */
public record DsrUpdateRequestDTO(
		@NotBlank String status,
		String resolution) {
}
