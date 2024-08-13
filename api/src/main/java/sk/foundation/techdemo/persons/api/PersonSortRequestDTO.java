package sk.foundation.techdemo.persons.api;

import sk.foundation.techdemo.infrastructure.api.SortRequestDTO;

public class PersonSortRequestDTO extends SortRequestDTO<PersonOrderBy> {

	public PersonSortRequestDTO(PersonOrderBy sortBy, boolean sortDesc) {
		super(sortBy, sortDesc);
	}

}
