package sk.foundation.techdemo.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sk.foundation.techdemo.infrastructure.api.ConflictException;

@ExtendWith(MockitoExtension.class)
class PersonMergeServiceTest {

	@Mock
	private PersonMergeRepository personMergeRepository;

	@Mock
	private RegistryPersonRepository personRepository;

	@InjectMocks
	private PersonMergeService service;

	@Test
	void rejectsSelfMerge() {
		assertThrows(ConflictException.class, () -> service.merge("P-1", "P-1", "r", "admin", "admin"));
	}

	@Test
	void rejectsAlreadyMergedDuplicate() {
		when(personRepository.lockByPersonUid(any())).thenReturn(Optional.of(new Person()));
		when(personMergeRepository.findByDuplicateUidAndSupersededAtIsNull("P-dup"))
				.thenReturn(Optional.of(new PersonMerge()));
		assertThrows(ConflictException.class, () -> service.merge("P-dup", "P-prim", "r", "admin", "admin"));
	}

	@Test
	void rejectsChainWhenPrimaryIsItselfMerged() {
		when(personRepository.lockByPersonUid(any())).thenReturn(Optional.of(new Person()));
		when(personMergeRepository.findByDuplicateUidAndSupersededAtIsNull("P-dup")).thenReturn(Optional.empty());
		when(personMergeRepository.existsByDuplicateUidAndSupersededAtIsNull("P-prim")).thenReturn(true);
		assertThrows(ConflictException.class, () -> service.merge("P-dup", "P-prim", "r", "admin", "admin"));
	}

	@Test
	void persistsValidMerge() {
		when(personRepository.lockByPersonUid(any())).thenReturn(Optional.of(new Person()));
		when(personMergeRepository.findByDuplicateUidAndSupersededAtIsNull("P-dup")).thenReturn(Optional.empty());
		when(personMergeRepository.existsByDuplicateUidAndSupersededAtIsNull("P-prim")).thenReturn(false);
		when(personMergeRepository.existsByPrimaryUidAndSupersededAtIsNull("P-dup")).thenReturn(false);
		when(personMergeRepository.save(any(PersonMerge.class))).thenAnswer(inv -> inv.getArgument(0));

		PersonMerge merge = service.merge("P-dup", "P-prim", "duplicate found", "admin", "supervisor");
		assertEquals("P-dup", merge.getDuplicateUid());
		assertEquals("P-prim", merge.getPrimaryUid());
	}

	@Test
	void resolveFollowsActiveMerge() {
		when(personMergeRepository.findByDuplicateUidAndSupersededAtIsNull("P-dup"))
				.thenReturn(Optional.of(merge("P-dup", "P-prim")));
		assertEquals("P-prim", service.resolvePersonUid("P-dup"));
	}

	@Test
	void resolveReturnsInputWhenNotMerged() {
		when(personMergeRepository.findByDuplicateUidAndSupersededAtIsNull("P-x")).thenReturn(Optional.empty());
		assertEquals("P-x", service.resolvePersonUid("P-x"));
	}

	private static PersonMerge merge(String duplicateUid, String primaryUid) {
		PersonMerge m = new PersonMerge();
		m.setDuplicateUid(duplicateUid);
		m.setPrimaryUid(primaryUid);
		return m;
	}
}
