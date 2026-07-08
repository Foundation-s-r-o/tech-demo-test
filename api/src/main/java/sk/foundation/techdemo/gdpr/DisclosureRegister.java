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
 * Art. 30 disclosure register (GDPR.DISCLOSURE_REGISTER): every hand-over of personal data.
 * Disclosed fields are stored as a CSV varchar.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "GDPR", name = "DISCLOSURE_REGISTER")
public class DisclosureRegister {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DISCLOSURE_ID", updatable = false)
	private Long disclosureId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "RECIPIENT", nullable = false)
	private String recipient;

	@Column(name = "RECIPIENT_TYPE", nullable = false)
	private String recipientType;

	@Column(name = "LEGAL_BASIS", nullable = false)
	private String legalBasis;

	@Column(name = "DISCLOSED_FIELDS", nullable = false)
	private String disclosedFields;

	@Column(name = "DISCLOSED_AT", nullable = false)
	private LocalDateTime disclosedAt;

	@Column(name = "DISCLOSED_BY", nullable = false)
	private String disclosedBy;

	@Column(name = "DSR_REQUEST_ID")
	private Long dsrRequestId;
}
