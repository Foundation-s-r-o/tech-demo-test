package sk.foundation.techdemo.persons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import sk.foundation.techdemo.TechDemoTestConfiguration;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("tc")
@Import(TechDemoTestConfiguration.class)
@Sql({ "/sql/data.sql" })
class PersonRepositoryIT {

	@Autowired
	PersonRepository personRepository;

	@Test
	void findByIdOfExistingUserShouldReturnData() {
		Optional<Person> userOpt = personRepository.findById(1L);

		assertTrue(userOpt.isPresent());
		Person user = userOpt.get();
		assertEquals("Admin", user.getFirstName());
		assertEquals("Admin", user.getLastName());
		assertEquals("admin@tech-demo.sk", user.getEmail());
	}

	@Test
	void savingUserWithNonUniqueEmailShouldThrow() {
		Person user1 = new Person();
		user1.setFirstName("FN1");
		user1.setLastName("LN1");
		user1.setEmail("u1@mail.sk");
		Person user2 = new Person();
		user2.setFirstName("FN2");
		user2.setLastName("LN2");
		user2.setEmail("u1@mail.sk");

		assertDoesNotThrow(() -> personRepository.persist(user1));
		assertThrows(DataIntegrityViolationException.class, () -> personRepository.persist(user2));
	}

}