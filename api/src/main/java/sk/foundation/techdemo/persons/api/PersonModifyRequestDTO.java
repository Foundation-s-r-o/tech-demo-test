package sk.foundation.techdemo.persons.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonModifyRequestDTO {

	// Required fields mirror the not-null columns on PERSON; the rest are optional (nullable columns).
	// Length is bounded with @Size (@Max is for numeric types and was silently ignored before Boot 4).

	@NotNull
	@Size(max = 255)
	private String firstName;

	@NotNull
	@Size(max = 255)
	private String lastName;

	@NotNull
	@Size(max = 255)
	private String email;

	@Size(max = 255)
	private String address;

	@Size(max = 255)
	private String state;

	@Size(max = 255)
	private String phoneNumber;

}
