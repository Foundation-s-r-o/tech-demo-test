package sk.foundation.techdemo.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Creates the known demo accounts only in explicitly selected local and integration-test profiles:
 * admin/admin (ROLE_ADMIN) and user/user (ROLE_USER, for authorization-matrix E2E tests).
 */
@Component
@Profile("local | it")
@RequiredArgsConstructor
public class LocalAdminInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) {
		seedAccount("admin", "admin", "Admin", "Admin", "ADMIN");
		seedAccount("user", "user", "User", "User", "USER");
	}

	private void seedAccount(String username, String password, String firstName, String lastName, String role) {
		User user = userRepository.findByUsername(username).orElseGet(User::new);
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setRole(role);
		user.setEnabled(true);
		userRepository.save(user);
	}
}
