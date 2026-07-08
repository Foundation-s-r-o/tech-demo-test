package sk.foundation.techdemo.auditlog;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;

/**
 * Security event (AUDIT.SECURITY_EVENT), NIS2: authn failures, privilege/config changes,
 * incidents, and CSIRT reporting timestamps.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "AUDIT", name = "SECURITY_EVENT")
public class SecurityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EVENT_ID", updatable = false)
	private Long eventId;

	@Column(name = "EVENT_TIME", nullable = false)
	private LocalDateTime eventTime;

	@Column(name = "EVENT_TYPE", nullable = false)
	private String eventType;

	@Column(name = "SEVERITY", nullable = false)
	private String severity;

	@Column(name = "ACTOR")
	private String actor;

	@Column(name = "SOURCE_ADDR")
	private String sourceAddr;

	@Lob
	@Column(name = "DETAILS")
	private String details;

	@Column(name = "INCIDENT_REF")
	private String incidentRef;

	@Column(name = "REPORTED_TO_CSIRT_AT")
	private LocalDateTime reportedToCsirtAt;
}
