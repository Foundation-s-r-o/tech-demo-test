package sk.foundation.techdemo.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Encrypts the national identifier (rodne cislo equivalent) with AES-256-GCM and derives a
 * deterministic keyed HMAC-SHA256 for exact-match lookup without decryption — the application
 * layer replaces the PostgreSQL {@code pgcrypto} ciphertext/HMAC columns.
 *
 * <p>Keys are 256-bit, base64-encoded, and read from configuration (env vars in production).
 * Fixed development keys are supplied only by the {@code local}/{@code it}/{@code e2e} profiles;
 * in every other profile a blank key fails fast at startup. See SECURITY.md.
 */
@Service
public class NationalIdCryptoService {

	private static final int GCM_TAG_BITS = 128;
	private static final int GCM_IV_BYTES = 12;
	private static final int AES_KEY_BYTES = 32;

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private final SecretKeySpec encKey;
	private final SecretKeySpec hmacKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public NationalIdCryptoService(
			@Value("${registry.crypto.national-id.enc-key:}") String encKeyBase64,
			@Value("${registry.crypto.national-id.hmac-key:}") String hmacKeyBase64) {
		if (!StringUtils.hasText(encKeyBase64) || !StringUtils.hasText(hmacKeyBase64)) {
			throw new IllegalStateException(
					"National-id crypto keys are not configured. Set registry.crypto.national-id.enc-key "
							+ "and .hmac-key (env vars in production; dev keys ship only with the "
							+ "local/it/e2e profiles).");
		}
		this.encKey = toAesKey(encKeyBase64);
		this.hmacKey = new SecretKeySpec(Base64.getDecoder().decode(hmacKeyBase64), "HmacSHA256");
	}

	/** Base64(iv || ciphertext+tag). Returns {@code null} for a null/blank input. */
	public String encrypt(String plaintext) {
		if (!StringUtils.hasText(plaintext)) {
			return null;
		}
		try {
			byte[] iv = new byte[GCM_IV_BYTES];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
			cipher.init(Cipher.ENCRYPT_MODE, encKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			byte[] out = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to encrypt national id", e);
		}
	}

	/** Inverse of {@link #encrypt}. Returns {@code null} for a null/blank input. */
	public String decrypt(String ciphertextBase64) {
		if (!StringUtils.hasText(ciphertextBase64)) {
			return null;
		}
		try {
			byte[] in = Base64.getDecoder().decode(ciphertextBase64);
			byte[] iv = new byte[GCM_IV_BYTES];
			System.arraycopy(in, 0, iv, 0, GCM_IV_BYTES);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
			cipher.init(Cipher.DECRYPT_MODE, encKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] plaintext = cipher.doFinal(in, GCM_IV_BYTES, in.length - GCM_IV_BYTES);
			return new String(plaintext, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to decrypt national id", e);
		}
	}

	/** Deterministic keyed HMAC (base64) for exact-match lookup. */
	public String hmac(String plaintext) {
		if (!StringUtils.hasText(plaintext)) {
			return null;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256", BouncyCastleProvider.PROVIDER_NAME);
			mac.init(hmacKey);
			return Base64.getEncoder().encodeToString(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to HMAC national id", e);
		}
	}

	private static SecretKeySpec toAesKey(String base64) {
		byte[] key = Base64.getDecoder().decode(base64);
		if (key.length != AES_KEY_BYTES) {
			throw new IllegalStateException("national-id enc-key must be a base64-encoded 256-bit key");
		}
		return new SecretKeySpec(key, "AES");
	}
}
