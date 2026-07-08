package sk.foundation.techdemo.registry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonAddressRepository extends JpaRepository<PersonAddress, Long> {

	List<PersonAddress> findByPersonUidAndSupersededAtIsNull(String personUid);

	/** All address versions of a person, newest recording first (Art. 15 export). */
	List<PersonAddress> findByPersonUidOrderByRecordedAtDesc(String personUid);
}
