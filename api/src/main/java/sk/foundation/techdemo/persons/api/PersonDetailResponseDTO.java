package sk.foundation.techdemo.persons.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PersonDetailResponseDTO {

	private final Long id;

	private final String firstName;

	private final String lastName;

	private final String email;

	private final String address;

	private final String state;

	private final String phoneNumber;

}
