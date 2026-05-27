package sk.foundation.techdemo.persons;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.persons.api.PersonModifyRequestDTO;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

	private final PersonRepository personRepository;
	private final Logger log;

	@Override
	@Transactional
	public Person persist(PersonModifyRequestDTO dto) {
		Person person = new Person();

		updateFromDTO(dto, person);
		personRepository.save(person);
		return person;
	}

	@Override
	@Transactional
	public Person update(Long id, PersonModifyRequestDTO dto) {
		Person person = personRepository.getReferenceById(id);

		updateFromDTO(dto, person);
		return person;
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		log.debug("Deleting person id={}", id);
		personRepository.deleteById(id);
	}

	private void updateFromDTO(PersonModifyRequestDTO dto, Person person) {
		person.setFirstName(dto.getFirstName());
		person.setLastName(dto.getLastName());
		person.setEmail(dto.getEmail());
		person.setAddress(dto.getAddress());
		person.setState(dto.getState());
		person.setPhoneNumber(dto.getPhoneNumber());
	}
}
