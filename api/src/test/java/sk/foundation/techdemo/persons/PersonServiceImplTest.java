package sk.foundation.techdemo.persons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.foundation.techdemo.persons.api.PersonModifyRequestDTO;

/**
 * Characterization tests pinning the CURRENT person service behavior before the upgrade.
 */
@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

	@Mock
	private PersonRepository personRepository;

	private final Logger log = LoggerFactory.getLogger(PersonServiceImplTest.class);

	private PersonServiceImpl personService;

	@BeforeEach
	void setUp() {
		personService = new PersonServiceImpl(personRepository, log);
	}

	private PersonModifyRequestDTO sampleDto() {
		PersonModifyRequestDTO dto = new PersonModifyRequestDTO();
		dto.setFirstName("Jozef");
		dto.setLastName("Antony");
		dto.setEmail("jozef@example.com");
		dto.setAddress("Bratislava");
		dto.setState("SK");
		dto.setPhoneNumber("0905548724");
		return dto;
	}

	@Test
	void persistUloziNovuOsobuSUdajmiZDto() {
		PersonModifyRequestDTO dto = sampleDto();

		Person result = personService.persist(dto);

		ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
		verify(personRepository).save(captor.capture());
		Person persisted = captor.getValue();
		assertThat(persisted).isSameAs(result);
		assertThat(result.getFirstName()).isEqualTo("Jozef");
		assertThat(result.getLastName()).isEqualTo("Antony");
		assertThat(result.getEmail()).isEqualTo("jozef@example.com");
		assertThat(result.getAddress()).isEqualTo("Bratislava");
		assertThat(result.getState()).isEqualTo("SK");
		assertThat(result.getPhoneNumber()).isEqualTo("0905548724");
	}

	@Test
	void updateNacitaReferenciuAPrepiseUdaje() {
		Person existing = new Person();
		when(personRepository.getReferenceById(1L)).thenReturn(existing);

		Person result = personService.update(1L, sampleDto());

		assertThat(result).isSameAs(existing);
		assertThat(result.getFirstName()).isEqualTo("Jozef");
		assertThat(result.getEmail()).isEqualTo("jozef@example.com");
	}

	@Test
	void deleteByIdDeleguje() {
		personService.deleteById(42L);

		verify(personRepository).deleteById(42L);
	}

	@Test
	void persistNiePrazdny() {
		Person result = personService.persist(sampleDto());

		assertThat(result).isNotNull();
		verify(personRepository).save(any(Person.class));
	}
}
