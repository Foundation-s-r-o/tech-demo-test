package sk.foundation.techdemo.registry;

import java.time.LocalDateTime;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.infrastructure.api.ConflictException;

/**
 * Person identity merge (duplicate -> primary) — the application-layer replacement for the
 * PostgreSQL merge-guard trigger, active-merge partial unique index, and
 * {@code resolve_person_uid()}. Merges are chain-free (single hop) and reversible via supersede.
 */
@Service
@RequiredArgsConstructor
public class PersonMergeService {

	private final PersonMergeRepository personMergeRepository;
	private final RegistryPersonRepository personRepository;

	@Transactional
	public PersonMerge merge(String duplicateUid, String primaryUid, String reason,
			String decidedBy, String approvedBy) {
		if (duplicateUid.equals(primaryUid)) {
			throw new ConflictException("Duplicate and primary person must differ");
		}
		// Serialize concurrent merges by pessimistically locking both person rows before the
		// uniqueness checks (same guard pattern as RegistryPersonApiService); lock in a
		// deterministic order to avoid deadlock when two merges touch the same pair.
		lockPersonsInDeterministicOrder(duplicateUid, primaryUid);
		if (personMergeRepository.findByDuplicateUidAndSupersededAtIsNull(duplicateUid).isPresent()) {
			throw new ConflictException("Person " + duplicateUid + " is already merged");
		}
		if (personMergeRepository.existsByDuplicateUidAndSupersededAtIsNull(primaryUid)) {
			throw new ConflictException("Primary " + primaryUid + " is itself merged (chains not allowed)");
		}
		if (personMergeRepository.existsByPrimaryUidAndSupersededAtIsNull(duplicateUid)) {
			throw new ConflictException("Duplicate " + duplicateUid + " is primary of another active merge");
		}
		PersonMerge merge = new PersonMerge();
		merge.setDuplicateUid(duplicateUid);
		merge.setPrimaryUid(primaryUid);
		merge.setReason(reason);
		merge.setMergedAt(LocalDateTime.now());
		merge.setDecidedBy(decidedBy);
		merge.setApprovedBy(approvedBy);
		return personMergeRepository.save(merge);
	}

	private void lockPersonsInDeterministicOrder(String uidA, String uidB) {
		String first = uidA.compareTo(uidB) <= 0 ? uidA : uidB;
		String second = first.equals(uidA) ? uidB : uidA;
		lockPerson(first);
		lockPerson(second);
	}

	private void lockPerson(String uid) {
		personRepository.lockByPersonUid(uid)
				.orElseThrow(() -> new EntityNotFoundException("Person not found: " + uid));
	}

	/** Follow the active merge link to the canonical identity (single hop by design). */
	@Transactional(readOnly = true)
	public String resolvePersonUid(String personUid) {
		return personMergeRepository.findByDuplicateUidAndSupersededAtIsNull(personUid)
				.map(PersonMerge::getPrimaryUid)
				.orElse(personUid);
	}
}
