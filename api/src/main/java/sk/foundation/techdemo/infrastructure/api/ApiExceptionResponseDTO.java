package sk.foundation.techdemo.infrastructure.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor
public class ApiExceptionResponseDTO {
	private final int code;

	@JsonIgnore
	private final HttpStatus httpStatus;

	private final String message;

	private final Map<String, List<String>> validationErrors;

	public ApiExceptionResponseDTO(int httpStatusCode, String message) {
		this(httpStatusCode, null, message, null);
	}

	public ApiExceptionResponseDTO(HttpStatus httpStatus, String message) {
		this(httpStatus.value(), httpStatus, message, null);
	}

	public ApiExceptionResponseDTO(HttpStatus httpStatus, String message, Map<String, List<String>> errors) {
		this(httpStatus.value(), httpStatus, message, errors);
	}

}
