package sk.foundation.techdemo.registry;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for the registry {@link Person} aggregate root. Named distinctly from the demo
 * {@code persons.PersonRepository} (removed in Stage 4) to avoid a Spring Data bean-name clash.
 */
public interface RegistryPersonRepository extends JpaRepository<Person, String> {

	Optional<Person> findByNationalIdHmac(String nationalIdHmac);

	/**
	 * Pessimistic lock on the person row — the serialization point for bitemporal writes
	 * (overlap/uniqueness checks then run against a stable current-belief snapshot).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from RegistryPerson p where p.personUid = :uid")
	Optional<Person> lockByPersonUid(@Param("uid") String uid);
}
