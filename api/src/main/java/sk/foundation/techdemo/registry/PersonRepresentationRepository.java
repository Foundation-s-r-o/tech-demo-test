package sk.foundation.techdemo.registry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepresentationRepository extends JpaRepository<PersonRepresentation, Long> {

	List<PersonRepresentation> findByRepresentativeUidAndSupersededAtIsNull(String representativeUid);
}
