package sk.foundation.techdemo.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonUidServiceTest {

	@Mock
	private RegistryPersonRepository personRepository;

	@InjectMocks
	private PersonUidService service;

	@Test
	void generatesUidThatValidates() {
		when(personRepository.existsById(anyString())).thenReturn(false);
		String uid = service.generate();
		assertTrue(uid.matches("P-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]{5}-[0-9A-HJKMNP-TV-Z]"));
		assertTrue(service.isValid(uid));
	}

	@Test
	void retriesOnCollision() {
		when(personRepository.existsById(anyString())).thenReturn(true, false);
		String uid = service.generate();
		assertTrue(service.isValid(uid));
		verify(personRepository, atLeast(2)).existsById(anyString());
	}

	@Test
	void rejectsTamperedCheckCharacter() {
		when(personRepository.existsById(anyString())).thenReturn(false);
		String uid = service.generate();
		char last = uid.charAt(14);
		char replacement = last == '0' ? '1' : '0';
		String tampered = uid.substring(0, 14) + replacement;
		assertFalse(service.isValid(tampered));
	}

	@Test
	void rejectsTamperedBody() {
		when(personRepository.existsById(anyString())).thenReturn(false);
		String uid = service.generate();
		char first = uid.charAt(2);
		char replacement = first == '2' ? '3' : '2';
		String tampered = uid.substring(0, 2) + replacement + uid.substring(3);
		assertFalse(service.isValid(tampered));
	}

	@Test
	void rejectsMalformedInput() {
		assertFalse(service.isValid(null));
		assertFalse(service.isValid(""));
		assertFalse(service.isValid("P-ABCDE-ABCDE-1")); // wrong length? 15 chars but bad checksum path
		assertFalse(service.isValid("X-12345-67890-0"));
		assertFalse(service.isValid("P-1234I-56789-0")); // contains excluded letter I
	}
}
