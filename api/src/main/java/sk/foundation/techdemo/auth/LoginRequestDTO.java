package sk.foundation.techdemo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequestDTO {
	private String username;
	private String password;
}
