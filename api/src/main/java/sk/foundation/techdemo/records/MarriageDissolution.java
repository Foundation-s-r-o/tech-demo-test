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
 * Dissolution of a marriage (REGISTRY.MARRIAGE_DISSOLUTION): divorce, annulment, or spouse death.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "MARRIAGE_DISSOLUTION")
public class MarriageDissolution extends NaturalKeyEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "DISSOLUTION_ID", updatable = false)
	private String dissolutionId;

	@Column(name = "MARRIAGE_ID", nullable = false)
	private String marriageId;

	@Column(name = "DISSOLUTION_TYPE", nullable = false)
	private String dissolutionType;

	@Column(name = "EFFECTIVE_DATE", nullable = false)
	private LocalDate effectiveDate;

	@Column(name = "COURT_CODE")
	private String courtCode;

	@Column(name = "COURT_REFERENCE")
	private String courtReference;

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

	@Override
	public String getId() {
		return dissolutionId;
	}
}
