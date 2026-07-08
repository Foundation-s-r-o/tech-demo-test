package sk.foundation.techdemo.registry;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.infrastructure.api.ConflictException;

/**
 * Application-layer replacement for the PostgreSQL GiST {@code EXCLUDE} constraints: enforces
 * that at most one current-belief bitemporal row exists per key at any point in valid time.
 * Callers must hold the person-row pessimistic lock ({@link RegistryPersonRepository#lockByPersonUid})
 * so the current-belief snapshot these checks read is stable within the write transaction.
 */
@Service
@RequiredArgsConstructor
public class BitemporalGuard {

	private final PersonDetailRepository personDetailRepository;
	private final PersonAddressRepository personAddressRepository;
	private final PersonReligionRepository personReligionRepository;
	private final PersonRepresentationRepository personRepresentationRepository;

	/** One current-belief identity-detail version per person per point in valid time. */
	public void assertDetailInsertable(String personUid, LocalDate validFrom, LocalDate validTo) {
		for (PersonDetail existing : personDetailRepository.findByPersonUidAndSupersededAtIsNull(personUid)) {
			if (overlaps(validFrom, validTo, existing.getValidFrom(), existing.getValidTo())) {
				throw new ConflictException(
						"Overlapping current identity detail for person " + personUid);
			}
		}
	}

	/** One current-belief address per person and address type per point in valid time. */
	public void assertAddressInsertable(String personUid, String addressType,
			LocalDate validFrom, LocalDate validTo) {
		for (PersonAddress existing : personAddressRepository.findByPersonUidAndSupersededAtIsNull(personUid)) {
			if (addressType.equals(existing.getAddressType())
					&& overlaps(validFrom, validTo, existing.getValidFrom(), existing.getValidTo())) {
				throw new ConflictException(
						"Overlapping current " + addressType + " address for person " + personUid);
			}
		}
	}

	/** One current-belief religion version per person per point in valid time. */
	public void assertReligionInsertable(String personUid, LocalDate validFrom, LocalDate validTo) {
		for (PersonReligion existing : personReligionRepository.findByPersonUidAndSupersededAtIsNull(personUid)) {
			if (overlaps(validFrom, validTo, existing.getValidFrom(), existing.getValidTo())) {
				throw new ConflictException(
						"Overlapping current religion for person " + personUid);
			}
		}
	}

	/** One current relationship of a given type per (represented, representative) pair. */
	public void assertRepresentationInsertable(String representedUid, String representativeUid,
			String type, LocalDate validFrom, LocalDate validTo) {
		List<PersonRepresentation> current =
				personRepresentationRepository.findByRepresentativeUidAndSupersededAtIsNull(representativeUid);
		for (PersonRepresentation existing : current) {
			if (representedUid.equals(existing.getRepresentedUid())
					&& type.equals(existing.getRepresentationType())
					&& overlaps(validFrom, validTo, existing.getValidFrom(), existing.getValidTo())) {
				throw new ConflictException("Overlapping current representation for " + representedUid);
			}
		}
	}

	/** Half-open valid-time interval overlap; a null upper bound means open-ended (+infinity). */
	static boolean overlaps(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
		boolean aStartsBeforeBEnds = bTo == null || aFrom.isBefore(bTo);
		boolean bStartsBeforeAEnds = aTo == null || bFrom.isBefore(aTo);
		return aStartsBeforeBEnds && bStartsBeforeAEnds;
	}
}
