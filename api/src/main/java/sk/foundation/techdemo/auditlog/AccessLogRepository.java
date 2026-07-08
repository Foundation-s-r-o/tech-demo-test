package sk.foundation.techdemo.auditlog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

	List<AccessLog> findByPersonUidOrderByAccessedAtDesc(String personUid);
}
