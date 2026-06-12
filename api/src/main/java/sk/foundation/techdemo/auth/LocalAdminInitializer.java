package sk.foundation.techdemo.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Creates the known demo account only in explicitly selected local and integration-test profiles. */
@Component
@Profile("local | it")
@RequiredArgsConstructor
public class LocalAdminInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) {
		User user = userRepository.findByUsername("admin").orElseGet(User::new);
		user.setUsername("admin");
		user.setPassword(passwordEncoder.encode("admin"));
		user.setFirstName("Admin");
		user.setLastName("Admin");
		user.setRole("ADMIN");
		user.setEnabled(true);
		userRepository.save(user);
	}
}
