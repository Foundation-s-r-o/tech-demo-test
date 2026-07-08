package sk.foundation.techdemo.registry;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;

/**
 * Court-ordered restriction of legal capacity (REGISTRY.LEGAL_CAPACITY_RESTRICTION), bitemporal.
 * Drives citizen-portal eligibility.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "LEGAL_CAPACITY_RESTRICTION")
public class LegalCapacityRestriction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RESTRICTION_ID", updatable = false)
	private Long restrictionId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "RESTRICTION_TYPE", nullable = false)
	private String restrictionType;

	@Column(name = "SCOPE")
	private String scope;

	@Column(name = "COURT_CODE")
	private String courtCode;

	@Column(name = "COURT_REFERENCE")
	private String courtReference;

	@Column(name = "VALID_FROM", nullable = false)
	private LocalDate validFrom;

	@Column(name = "VALID_TO")
	private LocalDate validTo;

	@Column(name = "RECORDED_AT", nullable = false)
	private LocalDateTime recordedAt;

	@Column(name = "RECORDED_BY", nullable = false)
	private String recordedBy;

	@Column(name = "SUPERSEDED_AT")
	private LocalDateTime supersededAt;

	@Column(name = "SUPERSEDED_BY")
	private Long supersededBy;

	@Column(name = "CHANGE_REASON")
	private String changeReason;
}
