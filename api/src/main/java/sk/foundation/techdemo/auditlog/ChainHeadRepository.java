package sk.foundation.techdemo.auditlog;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChainHeadRepository extends JpaRepository<ChainHead, Integer> {

	/** Serialize the hash chain: lock the single head row before appending. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from ChainHead c where c.id = :id")
	Optional<ChainHead> lockById(@Param("id") int id);
}
