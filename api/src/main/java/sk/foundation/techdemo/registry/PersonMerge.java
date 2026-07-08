package sk.foundation.techdemo.registry;

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
 * Identity merge link (REGISTRY.PERSON_MERGE): duplicate -> primary. Append-only and
 * reversible via {@code supersededAt}. Chain-free (a primary must not itself be a duplicate),
 * enforced by {@code PersonMergeService}.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "PERSON_MERGE")
public class PersonMerge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MERGE_ID", updatable = false)
	private Long mergeId;

	@Column(name = "DUPLICATE_UID", nullable = false, updatable = false)
	private String duplicateUid;

	@Column(name = "PRIMARY_UID", nullable = false, updatable = false)
	private String primaryUid;

	@Column(name = "REASON", nullable = false)
	private String reason;

	@Column(name = "MERGED_AT", nullable = false)
	private LocalDateTime mergedAt;

	@Column(name = "DECIDED_BY", nullable = false)
	private String decidedBy;

	@Column(name = "APPROVED_BY")
	private String approvedBy;

	@Column(name = "SUPERSEDED_AT")
	private LocalDateTime supersededAt;

	@Column(name = "REVERSAL_REASON")
	private String reversalReason;
}
