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
 * Tamper-evident row-change audit entry (AUDIT.AUDIT_LOG). Each row stores the previous chain
 * hash and its own SHA-256 hash over the payload, computed by {@code AuditService}.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "AUDIT", name = "AUDIT_LOG")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AUDIT_ID", updatable = false)
	private Long auditId;

	@Column(name = "EVENT_TIME", nullable = false)
	private LocalDateTime eventTime;

	@Column(name = "ACTOR", nullable = false)
	private String actor;

	@Column(name = "DB_ROLE", nullable = false)
	private String dbRole;

	@Column(name = "CLIENT_ADDR")
	private String clientAddr;

	@Column(name = "REQUEST_ID")
	private String requestId;

	@Column(name = "LEGAL_BASIS")
	private String legalBasis;

	@Column(name = "ACTION", nullable = false)
	private String action;

	@Column(name = "SCHEMA_NAME", nullable = false)
	private String schemaName;

	@Column(name = "TABLE_NAME", nullable = false)
	private String tableName;

	@Lob
	@Column(name = "OLD_ROW")
	private String oldRow;

	@Lob
	@Column(name = "NEW_ROW")
	private String newRow;

	@Column(name = "PREV_HASH", nullable = false)
	private String prevHash;

	@Column(name = "ROW_HASH", nullable = false)
	private String rowHash;
}
