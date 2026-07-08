package sk.foundation.techdemo.gdpr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Statutory retention rule (GDPR.RETENTION_POLICY), Art. 5(1)(e). Read-only reference data
 * seeded by migration V5.
 */
@Getter
@Setter
@Entity
@Table(schema = "GDPR", name = "RETENTION_POLICY")
public class RetentionPolicy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "POLICY_ID", updatable = false)
	private Long policyId;

	@Column(name = "RECORD_CLASS", nullable = false)
	private String recordClass;

	@Column(name = "RETENTION_YEARS", nullable = false)
	private Integer retentionYears;

	@Column(name = "LEGAL_REFERENCE", nullable = false)
	private String legalReference;

	@Column(name = "ACTION_AFTER", nullable = false)
	private String actionAfter;
}
