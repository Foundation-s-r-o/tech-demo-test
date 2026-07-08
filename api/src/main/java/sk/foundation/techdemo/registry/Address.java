package sk.foundation.techdemo.registry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.NaturalKeyEntity;

/**
 * Normalized address (REGISTRY.ADDRESS). Linked to persons bitemporally via
 * {@link PersonAddress}. The id is an application-generated UUID string.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "ADDRESS")
public class Address extends NaturalKeyEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ADDRESS_ID", updatable = false)
	private String addressId;

	@Column(name = "STREET")
	private String street;

	@Column(name = "HOUSE_NUMBER")
	private String houseNumber;

	@Column(name = "POSTAL_CODE")
	private String postalCode;

	@Column(name = "MUNICIPALITY_CODE")
	private String municipalityCode;

	@Column(name = "MUNICIPALITY_PART_CODE")
	private String municipalityPartCode;

	@Column(name = "COUNTRY_CODE", nullable = false)
	private String countryCode;

	@Column(name = "FOREIGN_ADDRESS")
	private String foreignAddress;

	@Override
	public String getId() {
		return addressId;
	}
}
