package sk.foundation.techdemo.auditlog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Tamper-evident row-change audit log with a SHA-256 hash chain — the application-layer
 * replacement for the PostgreSQL audit trigger + pgcrypto chain. Each entry stores the previous
 * chain hash and {@code rowHash = SHA-256(prevHash || payload)}; the single chain-head row is
 * pessimistically locked so concurrent writers serialize. Any retroactive edit breaks the chain,
 * which {@link #verifyChain()} detects.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

	/** Genesis hash seeded into AUDIT.CHAIN_HEAD by migration V5: SHA-256 of the string "genesis". */
	static final String GENESIS = sha256Hex("genesis");

	private final AuditLogRepository auditLogRepository;
	private final ChainHeadRepository chainHeadRepository;

	@Transactional
	public AuditLog record(
			String schemaName,
			String tableName,
			AuditAction action,
			String oldRow,
			String newRow,
			String actor,
			String dbRole,
			String requestId,
			String legalBasis) {
		ChainHead head = chainHeadRepository.lockById(ChainHead.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("Audit chain head row is missing"));
		String prevHash = head.getLastHash();
		LocalDateTime eventTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		String rowHash = computeRowHash(prevHash,
				payload(schemaName, tableName, action.name(), oldRow, newRow, eventTime));

		AuditLog entry = new AuditLog();
		entry.setEventTime(eventTime);
		entry.setActor(actor);
		entry.setDbRole(dbRole);
		entry.setRequestId(requestId);
		entry.setLegalBasis(legalBasis);
		entry.setAction(action.name());
		entry.setSchemaName(schemaName);
		entry.setTableName(tableName);
		entry.setOldRow(oldRow);
		entry.setNewRow(newRow);
		entry.setPrevHash(prevHash);
		entry.setRowHash(rowHash);
		AuditLog saved = auditLogRepository.save(entry);

		head.setLastHash(rowHash);
		chainHeadRepository.save(head);
		return saved;
	}

	/** Recompute the whole chain and report the first inconsistency, if any. */
	@Transactional(readOnly = true)
	public ChainVerificationResult verifyChain() {
		List<AuditLog> entries = auditLogRepository.findAllByOrderByAuditIdAsc();
		String expectedPrev = GENESIS;
		long checked = 0;
		for (AuditLog entry : entries) {
			checked++;
			String recomputed = computeRowHash(entry.getPrevHash(), payload(
					entry.getSchemaName(), entry.getTableName(), entry.getAction(),
					entry.getOldRow(), entry.getNewRow(), entry.getEventTime()));
			if (!expectedPrev.equals(entry.getPrevHash()) || !recomputed.equals(entry.getRowHash())) {
				return new ChainVerificationResult(false, checked, entry.getAuditId());
			}
			expectedPrev = entry.getRowHash();
		}
		// Tail check: the chain head must point at the last entry's hash. Catches truncation or
		// deletion of the newest row(s) — the per-row loop alone cannot see a row that is gone.
		ChainHead head = chainHeadRepository.findById(ChainHead.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("Audit chain head row is missing"));
		if (!expectedPrev.equals(head.getLastHash())) {
			return new ChainVerificationResult(false, checked, null);
		}
		return new ChainVerificationResult(true, checked, null);
	}

	static String computeRowHash(String prevHash, String payload) {
		return sha256Hex(prevHash + payload);
	}

	private static String payload(
			String schemaName, String tableName, String action,
			String oldRow, String newRow, LocalDateTime eventTime) {
		return String.join("|",
				schemaName, tableName, action,
				oldRow == null ? "" : oldRow,
				newRow == null ? "" : newRow,
				eventTime.toString());
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
