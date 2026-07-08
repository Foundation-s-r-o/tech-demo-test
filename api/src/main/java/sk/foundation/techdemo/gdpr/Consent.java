package sk.foundation.techdemo.gdpr;

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
 * GDPR consent record (GDPR.CONSENT) with withdrawal timestamp (Art. 7).
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "GDPR", name = "CONSENT")
public class Consent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CONSENT_ID", updatable = false)
	private Long consentId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "PURPOSE", nullable = false)
	private String purpose;

	@Column(name = "GIVEN_AT", nullable = false)
	private LocalDateTime givenAt;

	@Column(name = "GIVEN_VIA", nullable = false)
	private String givenVia;

	@Column(name = "WITHDRAWN_AT")
	private LocalDateTime withdrawnAt;

	@Column(name = "RECORDED_BY", nullable = false)
	private String recordedBy;
}
