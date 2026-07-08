package sk.foundation.techdemo.registry;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonMergeRepository extends JpaRepository<PersonMerge, Long> {

	/** The single active (non-reversed) merge for a duplicate, if any. */
	Optional<PersonMerge> findByDuplicateUidAndSupersededAtIsNull(String duplicateUid);

	boolean existsByPrimaryUidAndSupersededAtIsNull(String primaryUid);

	boolean existsByDuplicateUidAndSupersededAtIsNull(String duplicateUid);
}
