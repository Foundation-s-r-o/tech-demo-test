package sk.foundation.techdemo.persons;

import sk.foundation.techdemo.persons.api.PersonModifyRequestDTO;

public interface PersonService {

	Person persist(PersonModifyRequestDTO dto);

	Person update(Long id, PersonModifyRequestDTO dto);

	void deleteById(Long id);

}
