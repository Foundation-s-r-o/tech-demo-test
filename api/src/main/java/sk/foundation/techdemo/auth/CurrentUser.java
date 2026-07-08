package sk.foundation.techdemo.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Read the authenticated actor and role from the security context, for stamping created_by /
 * recorded_by fields and audit/access-log actor + role. Returns safe defaults when unauthenticated.
 */
public final class CurrentUser {

	private static final String UNKNOWN = "UNKNOWN";

	private CurrentUser() {
	}

	public static String username() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? UNKNOWN : authentication.getName();
	}

	public static String role() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return UNKNOWN;
		}
		return authentication.getAuthorities().stream()
				.findFirst()
				.map(GrantedAuthority::getAuthority)
				.orElse(UNKNOWN);
	}

	/** Registry office of the authenticated clerk, or {@code null} (central authority / non-clerk). */
	public static String officeCode() {
		return principal().map(AppUserDetails::getOfficeCode).orElse(null);
	}

	/** Person the authenticated citizen represents, or {@code null}. */
	public static String personUid() {
		return principal().map(AppUserDetails::getPersonUid).orElse(null);
	}

	private static java.util.Optional<AppUserDetails> principal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails details) {
			return java.util.Optional.of(details);
		}
		return java.util.Optional.empty();
	}
}
