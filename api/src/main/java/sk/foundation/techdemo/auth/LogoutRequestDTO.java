package sk.foundation.techdemo.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LogoutRequestDTO {
	private String username;
}
