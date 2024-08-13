package sk.foundation.techdemo.persons.api;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.infrastructure.api.PagedRequestDTO;
import sk.foundation.techdemo.infrastructure.api.PagedResultResponseDTO;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

	private final PersonApiService personApiService;

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PersonDetailResponseDTO> get(@PathVariable("id") Long id) {
		return new ResponseEntity<>(personApiService.getPerson(id), HttpStatus.OK);
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PagedResultResponseDTO<PersonListItemResponseDTO>> listUsers(
			@ParameterObject @ModelAttribute PersonFilter filter,
			@ParameterObject @ModelAttribute @Valid PagedRequestDTO paging,
			@ParameterObject @ModelAttribute PersonSortRequestDTO sorting) {
		return new ResponseEntity<>(personApiService.listPersons(filter, paging, sorting), HttpStatus.OK);
	}

	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO<Long>> create(@RequestBody @Valid PersonModifyRequestDTO dto) {
		return new ResponseEntity<>(personApiService.create(dto), HttpStatus.CREATED);
	}

	@PutMapping(value = "/{id}")
	public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody @Valid PersonModifyRequestDTO dto) {
		personApiService.update(id, dto);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		personApiService.delete(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
