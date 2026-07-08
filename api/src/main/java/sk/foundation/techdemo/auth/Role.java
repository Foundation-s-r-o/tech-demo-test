package sk.foundation.techdemo.auth;

/**
 * Civil-registry roles (the application-layer replacement for the PostgreSQL DB group roles).
 * Stored as the {@code USERS.ROLE} name; exposed to Spring Security as {@code ROLE_<name>}.
 *
 * <ul>
 * <li>{@code CLERK} — registers events and edits data for their own office</li>
 * <li>{@code SUPERVISOR} — nationwide read/write, approves amendments and merges</li>
 * <li>{@code AGENCY_READER} — minimal current-state view only (no Art. 9 / health)</li>
 * <li>{@code DPO_OFFICER} — full read + GDPR workflow, reads audit</li>
 * <li>{@code SECURITY_AUDITOR} — audit and security logs only</li>
 * <li>{@code CITIZEN} — self-service portal, own data only</li>
 * </ul>
 */
public enum Role {

	CLERK,
	SUPERVISOR,
	AGENCY_READER,
	DPO_OFFICER,
	SECURITY_AUDITOR,
	CITIZEN;

	public static final String PREFIX = "ROLE_";

	/** Spring Security authority string, e.g. {@code ROLE_CLERK}. */
	public String authority() {
		return PREFIX + name();
	}
}
