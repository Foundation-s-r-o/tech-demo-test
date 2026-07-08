package sk.foundation.techdemo.records;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.NaturalKeyEntity;

/**
 * Marriage record (REGISTRY.MARRIAGE_RECORD), append-only and transaction-time versioned.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "MARRIAGE_RECORD")
public class MarriageRecord extends NaturalKeyEntity<String> {

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

	@Column(name = "SPOUSE1_UID", nullable = false)
	private String spouse1Uid;

	@Column(name = "SPOUSE2_UID", nullable = false)
	private String spouse2Uid;

	@Column(name = "MARRIAGE_DATE", nullable = false)
	private LocalDate marriageDate;

	@Column(name = "PLACE_MUNICIPALITY")
	private String placeMunicipality;

	@Column(name = "PLACE_FOREIGN")
	private String placeForeign;

	@Column(name = "MARRIAGE_FORM", nullable = false)
	private String marriageForm;

	@Column(name = "OFFICIANT")
	private String officiant;

	@Column(name = "WITNESS1_NAME")
	private String witness1Name;

	@Column(name = "WITNESS2_NAME")
	private String witness2Name;

	@Column(name = "SURNAME_AGREEMENT")
	private String surnameAgreement;

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
