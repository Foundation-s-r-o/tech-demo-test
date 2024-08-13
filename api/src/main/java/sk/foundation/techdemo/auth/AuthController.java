package sk.foundation.techdemo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponseDTO> login(@RequestBody LoginRequestDTO dto) {
		UserResponseDTO userResponseDTO = authService.login(dto);
		return new ResponseEntity<>(userResponseDTO, HttpStatus.OK);
	}

	@GetMapping("/currentuser")
	public ResponseEntity<UserResponseDTO> currentUser() {
		UserResponseDTO userResponseDTO = authService.currentUser();
		return new ResponseEntity<>(userResponseDTO, HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody LogoutRequestDTO dto) {
		authService.logout(dto.getUsername());
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
