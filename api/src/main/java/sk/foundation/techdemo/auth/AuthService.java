package sk.foundation.techdemo.auth;

import sk.foundation.techdemo.auth.exception.UnauthorizedException;

public interface AuthService {
	UserResponseDTO login(LoginRequestDTO dto) throws UnauthorizedException;

	void logout(String username);

	UserResponseDTO currentUser();
}
