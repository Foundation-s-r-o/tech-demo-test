package sk.foundation.techdemo.infrastructure.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NationalIdCryptoServiceTest {

	private static final String ENC_KEY = "k8ZH9ltU1Fw+w+OpdOIkDfS7Ti9y2hcEjL5z8HCj85c=";
	private static final String HMAC_KEY = "bCb9VBgKIy2XpRgmRQ6ioGcacOtZgWsuJqFT5UNZ+Wk=";

	private final NationalIdCryptoService service = new NationalIdCryptoService(ENC_KEY, HMAC_KEY);

	@Test
	void encryptDecryptRoundTrips() {
		String plaintext = "8801011234";
		assertEquals(plaintext, service.decrypt(service.encrypt(plaintext)));
	}

	@Test
	void encryptionIsNonDeterministicButDecryptsConsistently() {
		String plaintext = "8801011234";
		String c1 = service.encrypt(plaintext);
		String c2 = service.encrypt(plaintext);
		assertNotEquals(c1, c2, "random IV should yield different ciphertexts");
		assertEquals(plaintext, service.decrypt(c1));
		assertEquals(plaintext, service.decrypt(c2));
	}

	@Test
	void hmacIsDeterministicForLookup() {
		assertEquals(service.hmac("8801011234"), service.hmac("8801011234"));
		assertNotEquals(service.hmac("8801011234"), service.hmac("9001011234"));
	}

	@Test
	void nullInputsReturnNull() {
		assertNull(service.encrypt(null));
		assertNull(service.decrypt(null));
		assertNull(service.hmac(" "));
	}

	@Test
	void missingKeysFailFast() {
		assertThrows(IllegalStateException.class, () -> new NationalIdCryptoService("", HMAC_KEY));
	}

	@Test
	void wrongKeyLengthFailsFast() {
		// base64 of 16 bytes, not the required 256-bit key
		assertThrows(IllegalStateException.class,
				() -> new NationalIdCryptoService("AAAAAAAAAAAAAAAAAAAAAA==", HMAC_KEY));
	}
}
