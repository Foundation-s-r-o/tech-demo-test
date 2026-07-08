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

/** Death records + Art. 9 cause-of-death (medical). Medical write is clerk/supervisor only. */
@RestController
@RequestMapping("/api/deaths")
@RequiredArgsConstructor
public class DeathController {

	static final String WRITE = "hasAnyRole('CLERK','SUPERVISOR')";
	static final String AMEND = "hasRole('SUPERVISOR')";
	static final String READ = "hasAnyRole('CLERK','SUPERVISOR','DPO_OFFICER')";
	private static final String PURPOSE = "X-Access-Purpose";
	private static final String REQUEST_ID = "X-Request-Id";

	private final VitalRecordsApiService service;

	@PostMapping
	@PreAuthorize(WRITE)
	public ResponseEntity<RecordIdResponseDTO> registerDeath(
			@RequestBody @Valid DeathRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(new RecordIdResponseDTO(service.registerDeath(dto, requestId)),
				HttpStatus.CREATED);
	}

	@PostMapping("/{id}/amendments")
	@PreAuthorize(AMEND)
	public ResponseEntity<RecordIdResponseDTO> amendDeath(
			@PathVariable("id") String id,
			@RequestBody @Valid DeathRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(new RecordIdResponseDTO(service.amendDeath(id, dto, requestId)),
				HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	@PreAuthorize(READ)
	public DeathResponseDTO getDeath(
			@PathVariable("id") String id,
			@RequestHeader(PURPOSE) String purpose,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return service.getDeath(id, purpose, requestId);
	}

	@PostMapping("/{id}/medical")
	@PreAuthorize(WRITE)
	public ResponseEntity<Void> setMedical(
			@PathVariable("id") String id,
			@RequestBody @Valid MedicalRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		service.setMedical(id, dto, requestId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/{id}/medical")
	@PreAuthorize(READ)
	public MedicalResponseDTO getMedical(
			@PathVariable("id") String id,
			@RequestHeader(PURPOSE) String purpose,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return service.getMedical(id, purpose, requestId);
	}
}
