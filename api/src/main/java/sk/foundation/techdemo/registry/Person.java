package sk.foundation.techdemo.registry;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.NaturalKeyEntity;

/**
 * Immutable identity anchor (REGISTRY.PERSON). Everything mutable lives in the bitemporal
 * satellites. The UID is application-generated ({@code PersonUidService}); the national id is
 * stored only as ciphertext + keyed HMAC ({@code NationalIdCryptoService}).
 */
@Getter
@Setter
@DynamicInsert
@Entity(name = "RegistryPerson")
@Table(schema = "REGISTRY", name = "PERSON")
public class Person extends NaturalKeyEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "PERSON_UID", updatable = false)
	private String personUid;

	@Column(name = "NATIONAL_ID_ENC")
	private String nationalIdEnc;

	@Column(name = "NATIONAL_ID_HMAC")
	private String nationalIdHmac;

	@Column(name = "ANONYMIZED", nullable = false)
	private boolean anonymized;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "CREATED_BY", nullable = false, updatable = false)
	private String createdBy;

	@Override
	public String getId() {
		return personUid;
	}
}
