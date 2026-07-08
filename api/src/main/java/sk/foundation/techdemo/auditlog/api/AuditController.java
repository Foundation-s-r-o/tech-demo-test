package sk.foundation.techdemo.auditlog.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auditlog.ChainVerificationResult;

/** Audit trail (NIS2 / GDPR accountability) — readable by DPO and security auditor. */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

	static final String AUDIT = "hasAnyRole('DPO_OFFICER','SECURITY_AUDITOR')";

	private final AuditReadService auditReadService;

	@GetMapping("/events")
	@PreAuthorize(AUDIT)
	public List<AuditEventDTO> events() {
		return auditReadService.events();
	}

	@GetMapping("/access-log")
	@PreAuthorize(AUDIT)
	public List<AccessLogEntryDTO> auditAccessLog(@RequestParam("personUid") String personUid) {
		return auditReadService.accessLog(personUid);
	}

	@GetMapping("/chain/verification")
	@PreAuthorize(AUDIT)
	public ChainVerificationResult chainVerification() {
		return auditReadService.verifyChain();
	}
}
