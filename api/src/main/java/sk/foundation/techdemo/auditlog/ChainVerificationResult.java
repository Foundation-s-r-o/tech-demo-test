package sk.foundation.techdemo.auditlog;

/**
 * Outcome of an audit hash-chain verification pass.
 *
 * @param intact           true if every link and row hash recomputed correctly
 * @param entriesChecked   number of audit-log rows examined
 * @param firstBrokenId    audit id of the first row that failed, or {@code null} if intact
 */
public record ChainVerificationResult(boolean intact, long entriesChecked, Long firstBrokenId) {
}
