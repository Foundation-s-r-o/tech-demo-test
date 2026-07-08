package sk.foundation.techdemo.records;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.NaturalKeyEntity;

/**
 * Birth record (REGISTRY.BIRTH_RECORD), append-only and transaction-time versioned.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "BIRTH_RECORD")
public class BirthRecord extends NaturalKeyEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "RECORD_ID", updatable = false)
	private String recordId;

	@Column(name = "OFFICE_CODE", nullable = false)
	private String officeCode;

	@Column(name = "BOOK_YEAR", nullable = false)
	private Integer bookYear;

	@Column(name = "ENTRY_NO", nullable = false)
	private Integer entryNo;

	@Column(name = "CHILD_UID", nullable = false)
	private String childUid;

	@Column(name = "DATE_OF_BIRTH", nullable = false)
	private LocalDate dateOfBirth;

	@Column(name = "TIME_OF_BIRTH")
	private LocalTime timeOfBirth;

	@Column(name = "PLACE_MUNICIPALITY")
	private String placeMunicipality;

	@Column(name = "PLACE_FOREIGN")
	private String placeForeign;

	@Column(name = "FACILITY")
	private String facility;

	@Column(name = "MULTIPLE_BIRTH_ORDER")
	private Integer multipleBirthOrder;

	@Column(name = "MOTHER_UID")
	private String motherUid;

	@Column(name = "FATHER_UID")
	private String fatherUid;

	@Column(name = "RECORDED_AT", nullable = false)
	private LocalDateTime recordedAt;

	@Column(name = "RECORDED_BY", nullable = false)
	private String recordedBy;

	@Column(name = "SUPERSEDED_AT")
	private LocalDateTime supersededAt;

	@Column(name = "SUPERSEDES_ID")
	private String supersedesId;

	@Column(name = "AMENDMENT_REASON")
	private String amendmentReason;

	@Column(name = "APPROVED_BY")
	private String approvedBy;

	@Override
	public String getId() {
		return recordId;
	}
}
