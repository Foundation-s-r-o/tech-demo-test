package sk.foundation.techdemo.auditlog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	/** Chain order for hash-chain verification. */
	List<AuditLog> findAllByOrderByAuditIdAsc();
}
