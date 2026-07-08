package sk.foundation.techdemo.registry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCapacityRestrictionRepository extends JpaRepository<LegalCapacityRestriction, Long> {

	List<LegalCapacityRestriction> findByPersonUidAndSupersededAtIsNull(String personUid);
}
