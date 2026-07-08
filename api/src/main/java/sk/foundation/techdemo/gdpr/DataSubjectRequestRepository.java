package sk.foundation.techdemo.gdpr;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, Long> {

	List<DataSubjectRequest> findByPersonUid(String personUid);
}
