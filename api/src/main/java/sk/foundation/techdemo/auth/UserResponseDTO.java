package sk.foundation.techdemo.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserResponseDTO {
	private final String firstName;
	private final String lastName;
	private final String username;
	private final String role;

	public static UserResponseDTO from(AppUserDetails principal) {
		return new UserResponseDTO(
				principal.getFirstName(),
				principal.getLastName(),
				principal.getUsername(),
				principal.getRole());
	}
}
