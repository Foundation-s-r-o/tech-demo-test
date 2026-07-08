package sk.foundation.techdemo.records;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BirthRecordRepository extends JpaRepository<BirthRecord, String> {

	List<BirthRecord> findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
			String officeCode, Integer bookYear, Integer entryNo);

	/** All birth records where the person is the child (Art. 15 export subject). */
	List<BirthRecord> findByChildUid(String childUid);
}
