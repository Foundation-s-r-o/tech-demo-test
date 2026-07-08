package sk.foundation.techdemo.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private ChainHeadRepository chainHeadRepository;

	@InjectMocks
	private AuditService service;

	@Test
	void genesisMatchesTheMigrationSeed() {
		// V5 seeds AUDIT.CHAIN_HEAD with sha256hex("genesis").
		assertEquals("aeebad4a796fcc2e15dc4c6061b45ed9b373f26adfc798ca7d2d8cc58182718e", AuditService.GENESIS);
	}

	@Test
	void rowHashIsDeterministicAndInputSensitive() {
		assertEquals(
				AuditService.computeRowHash("prev", "payload"),
				AuditService.computeRowHash("prev", "payload"));
		assertNotEquals(
				AuditService.computeRowHash("prev", "payload"),
				AuditService.computeRowHash("prev", "payload2"));
	}

	@Test
	void recordLinksToPreviousHashAndAdvancesHead() {
		ChainHead head = new ChainHead();
		head.setId(ChainHead.SINGLETON_ID);
		head.setLastHash(AuditService.GENESIS);
		when(chainHeadRepository.lockById(ChainHead.SINGLETON_ID)).thenReturn(Optional.of(head));
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

		service.record(
				"REGISTRY",
				"PERSON",
				AuditAction.INSERT,
				null,
				"{\"uid\":\"x\"}",
				"admin",
				"SUPERVISOR",
				"req-1",
				"art6_1c");

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		AuditLog saved = captor.getValue();
		assertEquals(AuditService.GENESIS, saved.getPrevHash());
		assertNotEquals(AuditService.GENESIS, saved.getRowHash());
		assertEquals(saved.getRowHash(), head.getLastHash(), "chain head advances to the new row hash");
		verify(chainHeadRepository).save(head);
	}
}
