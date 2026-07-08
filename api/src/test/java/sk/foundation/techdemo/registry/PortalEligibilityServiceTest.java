package sk.foundation.techdemo.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sk.foundation.techdemo.records.DeathRecordRepository;

@ExtendWith(MockitoExtension.class)
class PortalEligibilityServiceTest {

	@Mock
	private PersonDetailRepository personDetailRepository;
	@Mock
	private DeathRecordRepository deathRecordRepository;
	@Mock
	private LegalCapacityRestrictionRepository legalCapacityRestrictionRepository;
	@Mock
	private PersonRepresentationRepository personRepresentationRepository;

	@InjectMocks
	private PortalEligibilityService service;

	@Test
	void livingAdultWithoutRestrictionIsEligible() {
		markLivingAdult("P-1");
		assertTrue(service.isEligible("P-1"));
	}

	@Test
	void deceasedIsNotEligible() {
		when(deathRecordRepository.existsByPersonUidAndSupersededAtIsNull("P-1")).thenReturn(true);
		assertFalse(service.isEligible("P-1"));
	}

	@Test
	void minorIsNotEligible() {
		when(deathRecordRepository.existsByPersonUidAndSupersededAtIsNull("P-1")).thenReturn(false);
		when(personDetailRepository.findByPersonUidAndSupersededAtIsNull("P-1"))
				.thenReturn(List.of(detail(LocalDate.now().minusYears(10))));
		assertFalse(service.isEligible("P-1"));
	}

	@Test
	void restrictedCapacityIsNotEligible() {
		markLivingAdult("P-1");
		when(legalCapacityRestrictionRepository.findByPersonUidAndSupersededAtIsNull("P-1"))
				.thenReturn(List.of(restriction()));
		assertFalse(service.isEligible("P-1"));
	}

	@Test
	void ownDataIsAccessibleWhenEligible() {
		markLivingAdult("P-1");
		assertTrue(service.isUidAccessible("P-1", "P-1"));
	}

	@Test
	void guardianReachesRepresentedRegardlessOfAge() {
		markLivingAdult("P-guardian");
		when(personRepresentationRepository.findByRepresentativeUidAndSupersededAtIsNull("P-guardian"))
				.thenReturn(List.of(representation("P-ward", "guardian")));
		assertTrue(service.isUidAccessible("P-guardian", "P-ward"));
	}

	@Test
	void parentReachesMinorButNotAdultChild() {
		markLivingAdult("P-parent");
		when(personRepresentationRepository.findByRepresentativeUidAndSupersededAtIsNull("P-parent"))
				.thenReturn(List.of(representation("P-child", "parent")));
		// child is a minor -> accessible
		when(personDetailRepository.findByPersonUidAndSupersededAtIsNull("P-child"))
				.thenReturn(List.of(detail(LocalDate.now().minusYears(8))));
		assertTrue(service.isUidAccessible("P-parent", "P-child"));
	}

	@Test
	void parentDoesNotReachAdultChild() {
		markLivingAdult("P-parent");
		when(personRepresentationRepository.findByRepresentativeUidAndSupersededAtIsNull("P-parent"))
				.thenReturn(List.of(representation("P-child", "parent")));
		when(personDetailRepository.findByPersonUidAndSupersededAtIsNull("P-child"))
				.thenReturn(List.of(detail(LocalDate.now().minusYears(30))));
		assertFalse(service.isUidAccessible("P-parent", "P-child"));
	}

	private void markLivingAdult(String uid) {
		lenient().when(deathRecordRepository.existsByPersonUidAndSupersededAtIsNull(uid)).thenReturn(false);
		lenient().when(personDetailRepository.findByPersonUidAndSupersededAtIsNull(uid))
				.thenReturn(List.of(detail(LocalDate.now().minusYears(40))));
		lenient().when(legalCapacityRestrictionRepository.findByPersonUidAndSupersededAtIsNull(uid))
				.thenReturn(List.of());
	}

	private static PersonDetail detail(LocalDate dateOfBirth) {
		PersonDetail d = new PersonDetail();
		d.setValidFrom(LocalDate.parse("2000-01-01"));
		d.setValidTo(null);
		d.setDateOfBirth(dateOfBirth);
		return d;
	}

	private static LegalCapacityRestriction restriction() {
		LegalCapacityRestriction r = new LegalCapacityRestriction();
		r.setValidFrom(LocalDate.parse("2010-01-01"));
		r.setValidTo(null);
		return r;
	}

	private static PersonRepresentation representation(String representedUid, String type) {
		PersonRepresentation r = new PersonRepresentation();
		r.setRepresentedUid(representedUid);
		r.setRepresentationType(type);
		r.setValidFrom(LocalDate.parse("2015-01-01"));
		r.setValidTo(null);
		return r;
	}
}
