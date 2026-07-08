package sk.foundation.techdemo.registry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonReligionRepository extends JpaRepository<PersonReligion, Long> {

	List<PersonReligion> findByPersonUidAndSupersededAtIsNull(String personUid);

	/** Current religion rows backed by a given consent (superseded when the consent is withdrawn). */
	List<PersonReligion> findByConsentIdAndSupersededAtIsNull(Long consentId);

	/** All religion versions of a person, newest recording first (Art. 15 export). */
	List<PersonReligion> findByPersonUidOrderByRecordedAtDesc(String personUid);
}
