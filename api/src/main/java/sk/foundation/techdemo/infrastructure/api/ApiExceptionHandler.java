package sk.foundation.techdemo.infrastructure.api;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import sk.foundation.techdemo.auth.exception.UnauthorizedException;

/**
 * Centralized error handling. Emits RFC 7807 {@code application/problem+json} responses
 * (validation and other framework errors are already rendered as {@link ProblemDetail} by the
 * {@link ResponseEntityExceptionHandler} base class).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({ EntityNotFoundException.class, EmptyResultDataAccessException.class })
	public ProblemDetail handleNotFound(Exception ex) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	/**
	 * A database CHECK/constraint violation means the request carried invalid data (e.g. a death
	 * record with no date, found-date, or court declaration). Report 400 with a generic message —
	 * never leak SQL or constraint internals to the client.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Bad Request",
				"The request violates a data integrity constraint");
	}

	@ExceptionHandler({ AuthenticationException.class, UnauthorizedException.class })
	public ProblemDetail handleUnauthorized(Exception ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", "Access is denied");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
		problem.setTitle(title);
		return problem;
	}
}
