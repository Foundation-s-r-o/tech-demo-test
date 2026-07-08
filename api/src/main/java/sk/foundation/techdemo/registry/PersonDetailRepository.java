package sk.foundation.techdemo.registry;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonDetailRepository extends JpaRepository<PersonDetail, Long> {

	/** Current-belief versions (not yet superseded). */
	List<PersonDetail> findByPersonUidAndSupersededAtIsNull(String personUid);

	/** All versions of a person's identity detail, newest recording first. */
	List<PersonDetail> findByPersonUidOrderByRecordedAtDesc(String personUid);

	/**
	 * Search current-belief details by family name (case-insensitive substring), restricted to the
	 * version valid on {@code today} — so closed history copies (which keep the older name they
	 * held) never surface as stale search hits.
	 */
	@Query("select d from PersonDetail d where d.supersededAt is null "
			+ "and upper(d.familyName) like upper(concat('%', :familyName, '%')) "
			+ "and d.validFrom <= :today and (d.validTo is null or d.validTo > :today)")
	Page<PersonDetail> searchCurrent(@Param("familyName") String familyName,
			@Param("today") LocalDate today, Pageable pageable);
}
