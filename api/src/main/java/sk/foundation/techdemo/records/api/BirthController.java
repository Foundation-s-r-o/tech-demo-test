package sk.foundation.techdemo.records.api;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Birth records. Register: clerk (own office) / supervisor. Amend: supervisor. Read: +DPO. */
@RestController
@RequestMapping("/api/births")
@RequiredArgsConstructor
public class BirthController {

	static final String WRITE = "hasAnyRole('CLERK','SUPERVISOR')";
	static final String AMEND = "hasRole('SUPERVISOR')";
	static final String READ = "hasAnyRole('CLERK','SUPERVISOR','DPO_OFFICER')";
	private static final String PURPOSE = "X-Access-Purpose";
	private static final String REQUEST_ID = "X-Request-Id";

	private final VitalRecordsApiService service;

	@PostMapping
	@PreAuthorize(WRITE)
	public ResponseEntity<RecordIdResponseDTO> registerBirth(
			@RequestBody @Valid BirthRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(new RecordIdResponseDTO(service.registerBirth(dto, requestId)),
				HttpStatus.CREATED);
	}

	@PostMapping("/{id}/amendments")
	@PreAuthorize(AMEND)
	public ResponseEntity<RecordIdResponseDTO> amendBirth(
			@PathVariable("id") String id,
			@RequestBody @Valid BirthRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(new RecordIdResponseDTO(service.amendBirth(id, dto, requestId)),
				HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	@PreAuthorize(READ)
	public BirthResponseDTO getBirth(
			@PathVariable("id") String id,
			@RequestHeader(PURPOSE) String purpose,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return service.getBirth(id, purpose, requestId);
	}
}
