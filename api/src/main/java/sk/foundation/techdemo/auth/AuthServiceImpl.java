package sk.foundation.techdemo.auth;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import sk.foundation.techdemo.auth.exception.UnauthorizedException;

@Service
public class AuthServiceImpl implements AuthService {
	private static final UserResponseDTO MOCKED_USER = new UserResponseDTO("Admin", "Admin", "admin");
	private Queue<UserResponseDTO> mockedLoginQueue = new ConcurrentLinkedQueue<>();

	@Override
	public UserResponseDTO login(LoginRequestDTO dto) throws UnauthorizedException {
		if (dto.getUsername().equals(MOCKED_USER.getUsername())
				&& dto.getPassword().equals(MOCKED_USER.getPassword())) {
			boolean loggedIn = false;
			for (UserResponseDTO userResponseDTO : mockedLoginQueue) {
				if (userResponseDTO.getUsername().equals(dto.getUsername())) {
					loggedIn = true;
					break;
				}
			}
			if (!loggedIn) {
				mockedLoginQueue.add(MOCKED_USER);
			}
			return MOCKED_USER;
		}
		throw new UnauthorizedException("Invalid credentials");
	}

	@Override
	public void logout(String username) {
		for (UserResponseDTO userResponseDTO : mockedLoginQueue) {
			if (userResponseDTO.getUsername().equals(username)) {
				mockedLoginQueue.remove();
				break;
			}
		}
	}

	@Override
	public UserResponseDTO currentUser() throws sk.foundation.techdemo.auth.exception.UnauthorizedException {
		if (mockedLoginQueue.isEmpty()) throw new UnauthorizedException("User logged out");
		return MOCKED_USER;
	}
}
