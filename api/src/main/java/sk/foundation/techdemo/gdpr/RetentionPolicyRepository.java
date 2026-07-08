package sk.foundation.techdemo.gdpr;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {
}
