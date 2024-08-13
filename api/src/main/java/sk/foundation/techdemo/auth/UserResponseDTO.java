package sk.foundation.techdemo.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserResponseDTO {
	private final String firstName;
	private final String lastName;
	private final String username;
	@JsonIgnore
	private String password = "Passw0rd!";
}
