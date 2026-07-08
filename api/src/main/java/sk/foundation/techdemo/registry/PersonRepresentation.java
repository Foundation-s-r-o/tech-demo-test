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
 * Representation of one person by another (REGISTRY.PERSON_REPRESENTATION), bitemporal:
 * parent (while the child is a minor) or court-appointed guardian.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "PERSON_REPRESENTATION")
public class PersonRepresentation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REPRESENTATION_ID", updatable = false)
	private Long representationId;

	@Column(name = "REPRESENTED_UID", nullable = false, updatable = false)
	private String representedUid;

	@Column(name = "REPRESENTATIVE_UID", nullable = false, updatable = false)
	private String representativeUid;

	@Column(name = "REPRESENTATION_TYPE", nullable = false)
	private String representationType;

	@Column(name = "LEGAL_BASIS_REF")
	private String legalBasisRef;

	@Column(name = "COURT_CODE")
	private String courtCode;

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
