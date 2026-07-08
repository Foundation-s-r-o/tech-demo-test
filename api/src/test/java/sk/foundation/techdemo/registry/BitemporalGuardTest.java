package sk.foundation.techdemo.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sk.foundation.techdemo.infrastructure.api.ConflictException;

@ExtendWith(MockitoExtension.class)
class BitemporalGuardTest {

	@Mock
	private PersonDetailRepository personDetailRepository;
	@Mock
	private PersonAddressRepository personAddressRepository;
	@Mock
	private PersonReligionRepository personReligionRepository;
	@Mock
	private PersonRepresentationRepository personRepresentationRepository;

	@InjectMocks
	private BitemporalGuard guard;

	@Test
	void overlapHelperHandlesOpenEndedRanges() {
		assertTrue(BitemporalGuard.overlaps(
				LocalDate.parse("2021-01-01"), null, LocalDate.parse("2020-01-01"), null));
		assertFalse(BitemporalGuard.overlaps(
				LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31"),
				LocalDate.parse("2020-01-01"), null));
	}

	@Test
	void detailOverlapIsRejected() {
		when(personDetailRepository.findByPersonUidAndSupersededAtIsNull("P-1")).thenReturn(List.of(
				detail(LocalDate.parse("2020-01-01"), null)));
		assertThrows(ConflictException.class, () -> guard.assertDetailInsertable(
				"P-1", LocalDate.parse("2021-01-01"), null));
	}

	@Test
	void nonOverlappingDetailIsAllowed() {
		when(personDetailRepository.findByPersonUidAndSupersededAtIsNull("P-1")).thenReturn(List.of(
				detail(LocalDate.parse("2020-01-01"), null)));
		assertDoesNotThrow(() -> guard.assertDetailInsertable(
				"P-1", LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-31")));
	}

	@Test
	void addressOverlapIsTypeScoped() {
		when(personAddressRepository.findByPersonUidAndSupersededAtIsNull("P-1")).thenReturn(List.of(
				address("permanent", LocalDate.parse("2020-01-01"), null)));
		assertThrows(ConflictException.class, () -> guard.assertAddressInsertable(
				"P-1", "permanent", LocalDate.parse("2021-01-01"), null));
		assertDoesNotThrow(() -> guard.assertAddressInsertable(
				"P-1", "temporary", LocalDate.parse("2021-01-01"), null));
	}

	private static PersonDetail detail(LocalDate from, LocalDate to) {
		PersonDetail d = new PersonDetail();
		d.setValidFrom(from);
		d.setValidTo(to);
		return d;
	}

	private static PersonAddress address(String type, LocalDate from, LocalDate to) {
		PersonAddress a = new PersonAddress();
		a.setAddressType(type);
		a.setValidFrom(from);
		a.setValidTo(to);
		return a;
	}
}
