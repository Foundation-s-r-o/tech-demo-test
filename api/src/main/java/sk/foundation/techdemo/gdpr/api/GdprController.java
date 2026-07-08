package sk.foundation.techdemo.gdpr.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** GDPR workflow: DSRs, consents (+ withdrawal), disclosures, and Art. 15 export. */
@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GdprController {

	static final String DPO = "hasRole('DPO_OFFICER')";
	static final String CONSENT = "hasAnyRole('CLERK','SUPERVISOR','DPO_OFFICER')";
	private static final String REQUEST_ID = "X-Request-Id";

	private final GdprApiService gdprApiService;

	@PostMapping("/requests")
	@PreAuthorize(DPO)
	public ResponseEntity<Long> createRequest(
			@RequestBody @Valid DsrCreateRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(gdprApiService.createDsr(dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/requests")
	@PreAuthorize(DPO)
	public List<DsrResponseDTO> listRequests(@RequestParam("personUid") String personUid) {
		return gdprApiService.listDsr(personUid);
	}

	@PutMapping("/requests/{id}")
	@PreAuthorize(DPO)
	public DsrResponseDTO updateRequest(
			@PathVariable("id") Long id,
			@RequestBody @Valid DsrUpdateRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return gdprApiService.updateDsr(id, dto, requestId);
	}

	@PostMapping("/consents")
	@PreAuthorize(CONSENT)
	public ResponseEntity<Long> createConsent(
			@RequestBody @Valid ConsentCreateRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(gdprApiService.createConsent(dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/consents")
	@PreAuthorize(CONSENT)
	public List<ConsentResponseDTO> listConsents(@RequestParam("personUid") String personUid) {
		return gdprApiService.listConsent(personUid);
	}

	@PostMapping("/consents/{id}/withdrawal")
	@PreAuthorize(CONSENT)
	public ResponseEntity<Void> withdrawConsent(
			@PathVariable("id") Long id,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		gdprApiService.withdrawConsent(id, requestId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/disclosures")
	@PreAuthorize(DPO)
	public ResponseEntity<Long> createDisclosure(
			@RequestBody @Valid DisclosureCreateRequestDTO dto,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return new ResponseEntity<>(gdprApiService.createDisclosure(dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/disclosures")
	@PreAuthorize(DPO)
	public List<DisclosureResponseDTO> listDisclosures(@RequestParam("personUid") String personUid) {
		return gdprApiService.listDisclosure(personUid);
	}

	@GetMapping("/persons/{uid}/export")
	@PreAuthorize(DPO)
	public ExportResponseDTO export(
			@PathVariable("uid") String uid,
			@RequestHeader(value = REQUEST_ID, required = false) String requestId) {
		return gdprApiService.export(uid, requestId);
	}
}
