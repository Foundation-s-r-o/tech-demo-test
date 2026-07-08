package sk.foundation.techdemo.auditlog;

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
 * Read-access log (AUDIT.ACCESS_LOG) — who accessed which person's data and why (GDPR Art. 15).
 * Accessed fields are stored as a CSV varchar.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "AUDIT", name = "ACCESS_LOG")
public class AccessLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ACCESS_ID", updatable = false)
	private Long accessId;

	@Column(name = "ACCESSED_AT", nullable = false)
	private LocalDateTime accessedAt;

	@Column(name = "ACTOR", nullable = false)
	private String actor;

	@Column(name = "PERSON_UID", nullable = false)
	private String personUid;

	@Column(name = "PURPOSE", nullable = false)
	private String purpose;

	@Column(name = "LEGAL_BASIS")
	private String legalBasis;

	@Column(name = "FIELDS")
	private String fields;

	@Column(name = "REQUEST_ID")
	private String requestId;
}
