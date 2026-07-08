package sk.foundation.techdemo.registry;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Generates and validates the opaque person UID {@code P-XXXXX-XXXXX-C}:
 * 10 crypto-random Crockford base32 characters (no I/L/O/U — transcription-safe) plus a
 * position-weighted mod-32 check character. Java port of the PostgreSQL
 * {@code registry.generate_person_uid()} / {@code registry.person_uid_is_valid()} functions.
 */
@Service
@RequiredArgsConstructor
public class PersonUidService {

	/** Crockford base32 alphabet (excludes I, L, O, U). */
	static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

	private static final Pattern FORMAT = Pattern.compile(
			"P-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]");

	private static final int BODY_LENGTH = 10;

	private final SecureRandom secureRandom = new SecureRandom();

	private final RegistryPersonRepository personRepository;

	/** Generate a fresh, collision-free UID (retries on the astronomically unlikely clash). */
	public String generate() {
		String candidate;
		do {
			candidate = build(randomBody());
		} while (personRepository.existsById(candidate));
		return candidate;
	}

	/** Validate format and check character. */
	public boolean isValid(String uid) {
		if (uid == null || !FORMAT.matcher(uid).matches()) {
			return false;
		}
		String body = uid.substring(2, 7) + uid.substring(8, 13);
		return checkChar(body) == uid.charAt(14);
	}

	private String randomBody() {
		byte[] raw = new byte[BODY_LENGTH];
		secureRandom.nextBytes(raw);
		StringBuilder body = new StringBuilder(BODY_LENGTH);
		for (int i = 0; i < BODY_LENGTH; i++) {
			body.append(ALPHABET.charAt((raw[i] & 0xFF) % 32));
		}
		return body.toString();
	}

	private static String build(String body) {
		return "P-" + body.substring(0, 5) + "-" + body.substring(5, 10) + "-" + checkChar(body);
	}

	private static char checkChar(String body) {
		int checksum = 0;
		for (int i = 1; i <= BODY_LENGTH; i++) {
			checksum += i * ALPHABET.indexOf(body.charAt(i - 1));
		}
		return ALPHABET.charAt(checksum % 32);
	}
}
