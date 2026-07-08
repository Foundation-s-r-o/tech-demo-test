package sk.foundation.techdemo.registry.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.infrastructure.api.PagedRequestDTO;
import sk.foundation.techdemo.infrastructure.api.PagedResultResponseDTO;

/**
 * Civil-registry person API. Authorization is per-operation via {@link PreAuthorize}; personal-data
 * reads require the {@code X-Access-Purpose} header (missing -> 400 problem+json) and are
 * access-logged, with optional {@code X-Request-Id} correlation.
 */
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

	static final String WRITE = "hasAnyRole('CLERK','SUPERVISOR')";
	static final String READ = "hasAnyRole('CLERK','SUPERVISOR','DPO_OFFICER')";

	private static final String PURPOSE_HEADER = "X-Access-Purpose";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private final RegistryPersonApiService personApiService;
	private final RegistryPersonPdfService personPdfService;

	@PostMapping
	@PreAuthorize(WRITE)
	public ResponseEntity<UidResponseDTO> create(
			@RequestBody @Valid PersonCreateRequestDTO dto,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		String uid = personApiService.createPerson(dto, requestId);
		return new ResponseEntity<>(new UidResponseDTO(uid), HttpStatus.CREATED);
	}

	@GetMapping
	@PreAuthorize(READ)
	public PagedResultResponseDTO<PersonSummaryDTO> search(
			@RequestParam(name = "familyName", required = false) String familyName,
			@ParameterObject @ModelAttribute @Valid PagedRequestDTO paging,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.search(familyName, paging, purpose, requestId);
	}

	@GetMapping("/{uid}")
	@PreAuthorize(READ)
	public PersonCompositeResponseDTO getPerson(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.getComposite(uid, purpose, requestId);
	}

	@GetMapping(value = "/{uid}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@PreAuthorize(WRITE)
	public ResponseEntity<byte[]> pdf(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		byte[] pdf = personPdfService.generatePersonExtract(uid, purpose, requestId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.attachment().filename("person-" + uid + ".pdf").build());
		return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
	}

	@PostMapping("/{uid}/details")
	@PreAuthorize(WRITE)
	public ResponseEntity<Long> addDetail(
			@PathVariable("uid") String uid,
			@RequestBody @Valid PersonDetailRequestDTO dto,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return new ResponseEntity<>(personApiService.addDetail(uid, dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/{uid}/details")
	@PreAuthorize(READ)
	public List<DetailResponseDTO> getDetails(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.getDetailHistory(uid, purpose, requestId);
	}

	@PostMapping("/{uid}/addresses")
	@PreAuthorize(WRITE)
	public ResponseEntity<Long> addAddress(
			@PathVariable("uid") String uid,
			@RequestBody @Valid AddressRequestDTO dto,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return new ResponseEntity<>(personApiService.addAddress(uid, dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/{uid}/addresses")
	@PreAuthorize(READ)
	public List<AddressResponseDTO> getAddresses(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.getAddresses(uid, purpose, requestId);
	}

	@PostMapping("/{uid}/religion")
	@PreAuthorize(WRITE)
	public ResponseEntity<Long> addReligion(
			@PathVariable("uid") String uid,
			@RequestBody @Valid ReligionRequestDTO dto,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return new ResponseEntity<>(personApiService.addReligion(uid, dto, requestId), HttpStatus.CREATED);
	}

	@GetMapping("/{uid}/religion")
	@PreAuthorize(READ)
	public List<ReligionResponseDTO> getReligion(
			@PathVariable("uid") String uid,
			@RequestHeader(PURPOSE_HEADER) String purpose,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return personApiService.getReligion(uid, purpose, requestId);
	}
}
