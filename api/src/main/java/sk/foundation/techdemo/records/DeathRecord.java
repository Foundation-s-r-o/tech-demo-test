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
 * Death record (REGISTRY.DEATH_RECORD), append-only and transaction-time versioned. Cause of
 * death (health data, Art. 9) is isolated in {@link DeathMedical}.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "DEATH_RECORD")
public class DeathRecord extends NaturalKeyEntity<String> {

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

	@Column(name = "PERSON_UID", nullable = false)
	private String personUid;

	@Column(name = "DATE_OF_DEATH")
	private LocalDate dateOfDeath;

	@Column(name = "DATE_FOUND")
	private LocalDate dateFound;

	@Column(name = "DECLARED_BY_COURT", nullable = false)
	private boolean declaredByCourt;

	@Column(name = "COURT_CODE")
	private String courtCode;

	@Column(name = "COURT_REFERENCE")
	private String courtReference;

	@Column(name = "PLACE_MUNICIPALITY")
	private String placeMunicipality;

	@Column(name = "PLACE_FOREIGN")
	private String placeForeign;

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
