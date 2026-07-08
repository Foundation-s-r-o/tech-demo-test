package sk.foundation.techdemo.gdpr;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclosureRegisterRepository extends JpaRepository<DisclosureRegister, Long> {

	List<DisclosureRegister> findByPersonUid(String personUid);
}
