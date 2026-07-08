package sk.foundation.techdemo.auth;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sk.foundation.techdemo.auditlog.SecurityEvent;
import sk.foundation.techdemo.auditlog.SecurityEventRepository;

/**
 * Persists authentication-failure security events into AUDIT.SECURITY_EVENT (NIS2 monitoring
 * story). Records the attempted username, the failure type, and the source address when the
 * authentication carried web details — never a password or any other PII (logs-no-PII rule).
 *
 * <p>The audit write is best-effort: it runs inside the authentication flow, so a persistence
 * failure is logged and swallowed rather than propagated — a monitoring hiccup must never turn a
 * failed login into a different (or successful) outcome for the caller.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventListener {

	// Must match the AUDIT.SECURITY_EVENT CHECK constraints (V5): EVENT_TYPE + SEVERITY vocab.
	private static final String TYPE = "login_failure";
	private static final String SEVERITY = "warning";

	private final SecurityEventRepository securityEventRepository;

	@EventListener
	public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
		try {
			SecurityEvent securityEvent = new SecurityEvent();
			securityEvent.setEventTime(LocalDateTime.now());
			securityEvent.setEventType(TYPE);
			securityEvent.setSeverity(SEVERITY);
			securityEvent.setActor(event.getAuthentication().getName());
			securityEvent.setSourceAddr(sourceAddr(event));
			securityEvent.setDetails(event.getException().getClass().getSimpleName());
			securityEventRepository.save(securityEvent);
		} catch (RuntimeException ex) {
			// Never let a security-event write break the authentication flow.
			log.warn("Failed to persist authentication-failure security event: {}", ex.toString());
		}
	}

	private static String sourceAddr(AbstractAuthenticationFailureEvent event) {
		if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails web) {
			return web.getRemoteAddress();
		}
		return null;
	}
}
