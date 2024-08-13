package sk.foundation.techdemo.infrastructure.api;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(value = { EntityNotFoundException.class, EmptyResultDataAccessException.class })
	public ResponseEntity<Object> handleNotFound(
			Exception t,
			HttpServletRequest request,
			HttpServletResponse response) {
		String message = t.getMessage();
		return new ResponseEntity<>(new ApiExceptionResponseDTO(HttpStatus.NOT_FOUND, message), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(value = ConflictException.class)
	public ResponseEntity<Object> handleConflictException(
			Exception t,
			HttpServletRequest request,
			HttpServletResponse response) {
		return new ResponseEntity<>(
				new ApiExceptionResponseDTO(HttpStatus.CONFLICT, t.getMessage()),
				HttpStatus.CONFLICT);
	}

}
