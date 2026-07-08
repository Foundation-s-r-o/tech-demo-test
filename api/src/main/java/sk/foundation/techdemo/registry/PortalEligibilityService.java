package sk.foundation.techdemo.registry;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.records.DeathRecordRepository;

/**
 * Citizen self-service portal eligibility and representation access — the application-layer
 * equivalent of the PostgreSQL {@code citizen_is_eligible} / {@code citizen_portal_uid_accessible}
 * functions. Eligible = registered living person, adult (18+), no current capacity restriction.
 */
@Service
@RequiredArgsConstructor
public class PortalEligibilityService {

	private static final int ADULT_AGE_YEARS = 18;
	private static final String TYPE_GUARDIAN = "guardian";
	private static final String TYPE_PARENT = "parent";

	private final PersonDetailRepository personDetailRepository;
	private final DeathRecordRepository deathRecordRepository;
	private final LegalCapacityRestrictionRepository legalCapacityRestrictionRepository;
	private final PersonRepresentationRepository personRepresentationRepository;

	@Transactional(readOnly = true)
	public boolean isEligible(String personUid) {
		if (personUid == null) {
			return false;
		}
		LocalDate today = LocalDate.now();
		if (deathRecordRepository.existsByPersonUidAndSupersededAtIsNull(personUid)) {
			return false;
		}
		Optional<PersonDetail> currentDetail = currentDetail(personUid, today);
		if (currentDetail.isEmpty() || !isAdult(currentDetail.get(), today)) {
			return false;
		}
		return legalCapacityRestrictionRepository.findByPersonUidAndSupersededAtIsNull(personUid).stream()
				.noneMatch(r -> currentInValidTime(r.getValidFrom(), r.getValidTo(), today));
	}

	/**
	 * May the authenticated citizen access the target person's data — own data if eligible, or a
	 * represented person's (guardian always; parent while the represented person is a minor)?
	 */
	@Transactional(readOnly = true)
	public boolean isUidAccessible(String citizenUid, String targetUid) {
		if (citizenUid == null || targetUid == null || !isEligible(citizenUid)) {
			return false;
		}
		if (targetUid.equals(citizenUid)) {
			return true;
		}
		LocalDate today = LocalDate.now();
		return personRepresentationRepository.findByRepresentativeUidAndSupersededAtIsNull(citizenUid).stream()
				.filter(r -> targetUid.equals(r.getRepresentedUid()))
				.filter(r -> currentInValidTime(r.getValidFrom(), r.getValidTo(), today))
				.anyMatch(r -> TYPE_GUARDIAN.equals(r.getRepresentationType())
						|| (TYPE_PARENT.equals(r.getRepresentationType()) && isMinor(targetUid, today)));
	}

	private boolean isMinor(String personUid, LocalDate today) {
		return currentDetail(personUid, today)
				.map(d -> !isAdult(d, today))
				.orElse(false);
	}

	private Optional<PersonDetail> currentDetail(String personUid, LocalDate today) {
		return personDetailRepository.findByPersonUidAndSupersededAtIsNull(personUid).stream()
				.filter(d -> currentInValidTime(d.getValidFrom(), d.getValidTo(), today))
				.findFirst();
	}

	private static boolean isAdult(PersonDetail detail, LocalDate today) {
		return !detail.getDateOfBirth().plusYears(ADULT_AGE_YEARS).isAfter(today);
	}

	private static boolean currentInValidTime(LocalDate validFrom, LocalDate validTo, LocalDate today) {
		return !validFrom.isAfter(today) && (validTo == null || validTo.isAfter(today));
	}
}
