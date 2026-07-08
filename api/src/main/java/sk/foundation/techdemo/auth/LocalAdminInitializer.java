package sk.foundation.techdemo.auth;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import sk.foundation.techdemo.registry.Person;
import sk.foundation.techdemo.registry.PersonDetail;
import sk.foundation.techdemo.registry.PersonDetailRepository;
import sk.foundation.techdemo.registry.PersonUidService;
import sk.foundation.techdemo.registry.RegistryPersonRepository;

/**
 * Seeds the known demo accounts only in the explicitly selected local and integration-test
 * profiles, one per registry role: admin ({@link Role#SUPERVISOR}), user ({@link Role#CLERK} @
 * Bratislava office), dpo, auditor, agency, and a citizen linked to a portal-eligible person.
 */
@Component
@Profile("local | it")
@RequiredArgsConstructor
public class LocalAdminInitializer implements ApplicationRunner {

	/** Bratislava - Stare Mesto registry office (CL000015), for the clerk's office scope. */
	private static final String CLERK_OFFICE = "2957";
	// Demo seed identities (local/it profiles only): the username doubles as the password and,
	// for the admin, as the seeding actor recorded in created_by/recorded_by. See SECURITY.md.
	private static final String ADMIN = "admin";
	private static final String CITIZEN = "citizen";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RegistryPersonRepository personRepository;
	private final PersonDetailRepository personDetailRepository;
	private final PersonUidService personUidService;

	@Override
	public void run(ApplicationArguments args) {
		seedAccount(ADMIN, ADMIN, "Admin", "Admin", Role.SUPERVISOR.name(), null, null);
		seedAccount("user", "user", "User", "User", Role.CLERK.name(), CLERK_OFFICE, null);
		seedAccount("dpo", "dpo", "Dpo", "Officer", Role.DPO_OFFICER.name(), null, null);
		seedAccount("auditor", "auditor", "Security", "Auditor", Role.SECURITY_AUDITOR.name(), null, null);
		seedAccount("agency", "agency", "Agency", "Reader", Role.AGENCY_READER.name(), null, null);

		String citizenUid = userRepository.findByUsername(CITIZEN)
				.map(User::getPersonUid)
				.filter(StringUtils::hasText)
				.orElseGet(this::createCitizenPerson);
		seedAccount(CITIZEN, CITIZEN, "Citizen", "Portal", Role.CITIZEN.name(), null, citizenUid);
	}

	/** Create a living adult person (Slovak citizenship) for the citizen portal login to represent. */
	private String createCitizenPerson() {
		String uid = personUidService.generate();
		Person person = new Person();
		person.setPersonUid(uid);
		person.setAnonymized(false);
		person.setCreatedAt(LocalDateTime.now());
		person.setCreatedBy(ADMIN);
		personRepository.save(person);

		PersonDetail detail = new PersonDetail();
		detail.setPersonUid(uid);
		detail.setGivenNames("Obcan");
		detail.setFamilyName("Portalovy");
		detail.setSex("M");
		detail.setDateOfBirth(LocalDate.of(1990, 1, 1));
		detail.setDateOfBirthPrecision("day");
		detail.setCitizenship("703");
		detail.setValidFrom(LocalDate.of(2008, 1, 1));
		detail.setRecordedAt(LocalDateTime.now());
		detail.setRecordedBy(ADMIN);
		personDetailRepository.save(detail);
		return uid;
	}

	private void seedAccount(String username, String password, String firstName, String lastName,
			String role, String officeCode, String personUid) {
		User user = userRepository.findByUsername(username).orElseGet(User::new);
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setRole(role);
		user.setEnabled(true);
		user.setOfficeCode(officeCode);
		user.setPersonUid(personUid);
		userRepository.save(user);
	}
}
