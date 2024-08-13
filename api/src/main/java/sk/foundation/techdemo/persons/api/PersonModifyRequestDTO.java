package sk.foundation.techdemo.persons.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonModifyRequestDTO {

	@NotNull
	@Max(256)
	private String firstName;

	@NotNull
	@Max(256)
	private String lastName;

	@NotNull
	@Max(256)
	private String email;

	@NotNull
	@Max(256)
	private String address;

	@Max(256)
	private String state;

	@Max(256)
	private String phoneNumber;

}
