package sk.foundation.techdemo.auditlog;

/**
 * Row-change action recorded in the audit log (mirrors the AUDIT.AUDIT_LOG {@code ACTION} check).
 */
public enum AuditAction {
	INSERT,
	UPDATE,
	DELETE
}
