package sk.foundation.techdemo.registry.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Read-only agency API (T9). The AGENCY_READER role sees only the minimal current-state projection
 * ({@link AgencyPersonDTO}) — no Art. 9 data, history, or vital records. Reads require the
 * {@code X-Access-Purpose} header (missing -> 400 problem+json) and are access-logged.
 */
@RestController
@RequestMapping("/api/agency/persons")
@RequiredArgsConstructor
public class AgencyController {

	private static final String PURPOSE_HEADER = "X-Access-Purpose";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private final RegistryPersonApiService personApiService;

	@GetMapping("/{uid}")
	@PreAuthorize("hasRole('AGENCY_READER')")
	public AgencyPersonDTO getPerson(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.agencyView(uid, purpose, requestId);
	}
}
