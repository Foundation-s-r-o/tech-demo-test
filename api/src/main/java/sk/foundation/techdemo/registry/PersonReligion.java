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
 * Bitemporal religion (REGISTRY.PERSON_RELIGION) — GDPR Art. 9 special category. Each row
 * carries its Art. 9(2) legal basis; consent-based rows reference a {@code gdpr.consent} row.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "PERSON_RELIGION")
public class PersonReligion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PERSON_RELIGION_ID", updatable = false)
	private Long personReligionId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "RELIGION_CODE", nullable = false)
	private String religionCode;

	@Column(name = "LEGAL_BASIS", nullable = false)
	private String legalBasis;

	@Column(name = "CONSENT_ID")
	private Long consentId;

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
