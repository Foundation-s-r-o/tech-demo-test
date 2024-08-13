package sk.foundation.techdemo.persons;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
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
	@CacheEvict(value = "personlist")
	public Person persist(PersonModifyRequestDTO dto) {
		Person person = new Person();

		updateFromDTO(dto, person);
		personRepository.persist(person);
		log.info("Trace Id: {}", Span.current().getSpanContext().getTraceId());
		log.info("Span Id: {}", Span.current().getSpanContext().getSpanId());
		return person;
	}

	@Override
	@Transactional
	@CacheEvict(value = { "persondetail", "personlist" }, key = "#id")
	public Person update(Long id, PersonModifyRequestDTO dto) {
		Person person = personRepository.getReferenceById(id);

		updateFromDTO(dto, person);
		log.info("Trace Id: {}", Span.current().getSpanContext().getTraceId());
		log.info("Span Id: {}", Span.current().getSpanContext().getSpanId());
		return person;
	}

	@Override
	@Transactional
	@CacheEvict(value = { "persondetail", "personlist" }, key = "#id")
	public void deleteById(Long id) {
		log.info("Trace Id: {}", Span.current().getSpanContext().getTraceId());
		log.info("Span Id: {}", Span.current().getSpanContext().getSpanId());
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
