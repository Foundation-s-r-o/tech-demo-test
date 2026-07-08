package sk.foundation.techdemo.gdpr;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

	List<Consent> findByPersonUid(String personUid);
}
