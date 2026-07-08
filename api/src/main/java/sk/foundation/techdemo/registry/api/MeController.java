package sk.foundation.techdemo.registry.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.api.AccessLogEntryDTO;

/**
 * Citizen self-service portal — own data only. The person is resolved from the authenticated
 * principal ({@code USERS.PERSON_UID}); access is gated by portal eligibility (living adult,
 * unrestricted capacity).
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

	static final String CITIZEN = "hasRole('CITIZEN')";
	private static final String PURPOSE_HEADER = "X-Access-Purpose";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private final RegistryPersonApiService personApiService;

	@GetMapping
	@PreAuthorize(CITIZEN)
	public PersonCompositeResponseDTO me(
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.getMe(purpose, requestId);
	}

	@GetMapping("/access-log")
	@PreAuthorize(CITIZEN)
	public List<AccessLogEntryDTO> myAccessLog() {
		return personApiService.meAccessLog();
	}
}
