package sk.foundation.techdemo.records;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarriageRecordRepository extends JpaRepository<MarriageRecord, String> {

	List<MarriageRecord> findByOfficeCodeAndBookYearAndEntryNoAndSupersededAtIsNull(
			String officeCode, Integer bookYear, Integer entryNo);

	/** All marriage records where the person is either spouse (Art. 15 export subject). */
	List<MarriageRecord> findBySpouse1UidOrSpouse2Uid(String spouse1Uid, String spouse2Uid);
}
