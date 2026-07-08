package sk.foundation.techdemo.gdpr.api;

import jakarta.validation.constraints.NotBlank;

/** File a data subject request. The statutory due date is computed by the service. */
public record DsrCreateRequestDTO(
		String personUid,
		@NotBlank String requesterRef,
		@NotBlank String requestType) {
}
