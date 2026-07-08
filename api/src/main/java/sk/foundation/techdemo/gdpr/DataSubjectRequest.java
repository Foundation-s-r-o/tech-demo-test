package sk.foundation.techdemo.gdpr;

import java.time.LocalDate;

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
 * Data subject request (GDPR.DATA_SUBJECT_REQUEST), Art. 15-21, with the statutory one-month
 * due date computed by the service on create.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "GDPR", name = "DATA_SUBJECT_REQUEST")
public class DataSubjectRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REQUEST_ID", updatable = false)
	private Long requestId;

	@Column(name = "PERSON_UID")
	private String personUid;

	@Column(name = "REQUESTER_REF", nullable = false)
	private String requesterRef;

	@Column(name = "REQUEST_TYPE", nullable = false)
	private String requestType;

	@Column(name = "RECEIVED_AT", nullable = false)
	private LocalDate receivedAt;

	@Column(name = "DUE_AT", nullable = false)
	private LocalDate dueAt;

	@Column(name = "STATUS", nullable = false)
	private String status;

	@Column(name = "HANDLED_BY")
	private String handledBy;

	@Column(name = "RESOLVED_AT")
	private LocalDate resolvedAt;

	@Column(name = "RESOLUTION")
	private String resolution;
}
