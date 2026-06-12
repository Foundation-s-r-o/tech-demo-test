package sk.foundation.techdemo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.auth.exception.UnauthorizedException;

/**
 * Session-based login backed by {@link AppUserDetailsService}. Preserves the original
 * {@code /api/auth} JSON contract used by the SPA.
 */
@RestController
@RequestMapping(path = "/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken csrfToken) {
		return csrfToken;
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponseDTO> login(
			@RequestBody LoginRequestDTO dto,
			HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		return ResponseEntity.ok(UserResponseDTO.from((AppUserDetails) authentication.getPrincipal()));
	}

	@GetMapping("/currentuser")
	public ResponseEntity<UserResponseDTO> currentUser(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
			throw new UnauthorizedException("Not authenticated");
		}
		return ResponseEntity.ok(UserResponseDTO.from(principal));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
