package sk.foundation.techdemo.persons.api;

import jakarta.validation.Valid;

import sk.foundation.techdemo.infrastructure.api.PagedRequestDTO;
import sk.foundation.techdemo.infrastructure.api.PagedResultResponseDTO;

public interface PersonApiService {

	PagedResultResponseDTO<PersonListItemResponseDTO> listPersons(
			PersonFilter filter,
			PagedRequestDTO paging,
			PersonSortRequestDTO sorting);

	PersonDetailResponseDTO getPerson(Long id);

	IdResponseDTO<Long> create(@Valid PersonModifyRequestDTO dto);

	void update(Long id, @Valid PersonModifyRequestDTO dto);

	void delete(Long id);

}
