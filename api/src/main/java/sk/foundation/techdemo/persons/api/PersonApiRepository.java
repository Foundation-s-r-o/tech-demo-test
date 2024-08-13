package sk.foundation.techdemo.persons.api;

import java.util.List;

import sk.foundation.techdemo.infrastructure.api.PagedRequestDTO;

public interface PersonApiRepository {

	List<PersonListItemResponseDTO> listPersons(
			PersonFilter filter,
			PagedRequestDTO paging,
			PersonSortRequestDTO sorting);

	Long getTotalPersons(PersonFilter filter);

}
