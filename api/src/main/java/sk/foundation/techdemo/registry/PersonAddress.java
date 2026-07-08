package sk.foundation.techdemo.registry;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import lombok.Getter;
import lombok.Setter;

/**
 * Bitemporal person-to-address link (REGISTRY.PERSON_ADDRESS), typed by address type.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "PERSON_ADDRESS")
public class PersonAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PERSON_ADDRESS_ID", updatable = false)
	private Long personAddressId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "ADDRESS_ID", nullable = false)
	private String addressId;

	@Column(name = "ADDRESS_TYPE", nullable = false)
	private String addressType;

	@Column(name = "VALID_FROM", nullable = false)
	private LocalDate validFrom;

	@Column(name = "VALID_TO")
	private LocalDate validTo;

	@Column(name = "RECORDED_AT", nullable = false)
	private LocalDateTime recordedAt;

	@Column(name = "RECORDED_BY", nullable = false)
	private String recordedBy;

	@Column(name = "SUPERSEDED_AT")
	private LocalDateTime supersededAt;

	@Column(name = "SUPERSEDED_BY")
	private Long supersededBy;

	@Column(name = "CHANGE_REASON")
	private String changeReason;
}
