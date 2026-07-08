package sk.foundation.techdemo.records;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeathRecordRepository extends JpaRepository<DeathRecord, String> {

	boolean existsByPersonUidAndSupersededAtIsNull(String personUid);

	List<DeathRecord> findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
			String officeCode, Integer bookYear, Integer entryNo);

	/** All death records for the person (Art. 15 export subject). */
	List<DeathRecord> findByPersonUid(String personUid);
}
