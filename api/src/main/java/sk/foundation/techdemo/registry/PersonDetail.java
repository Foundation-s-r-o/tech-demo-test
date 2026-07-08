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
 * Bitemporal identity details (REGISTRY.PERSON_DETAIL): names, sex, birth data, citizenship.
 * Never updated in place except to set {@code supersededAt}/{@code supersededBy}.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "PERSON_DETAIL")
public class PersonDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DETAIL_ID", updatable = false)
	private Long detailId;

	@Column(name = "PERSON_UID", nullable = false, updatable = false)
	private String personUid;

	@Column(name = "GIVEN_NAMES", nullable = false)
	private String givenNames;

	@Column(name = "FAMILY_NAME", nullable = false)
	private String familyName;

	@Column(name = "BIRTH_SURNAME")
	private String birthSurname;

	@Column(name = "SEX", nullable = false)
	private String sex;

	@Column(name = "DATE_OF_BIRTH", nullable = false)
	private LocalDate dateOfBirth;

	@Column(name = "DATE_OF_BIRTH_PRECISION", nullable = false)
	private String dateOfBirthPrecision;

	@Column(name = "PLACE_OF_BIRTH_MUNICIPALITY")
	private String placeOfBirthMunicipality;

	@Column(name = "PLACE_OF_BIRTH_FOREIGN")
	private String placeOfBirthForeign;

	@Column(name = "CITIZENSHIP", nullable = false)
	private String citizenship;

	@Column(name = "MARITAL_STATUS_CODE")
	private String maritalStatusCode;

	@Column(name = "EXISTENCE_STATUS_CODE")
	private String existenceStatusCode;

	@Column(name = "TITLES_BEFORE")
	private String titlesBefore;

	@Column(name = "TITLES_AFTER")
	private String titlesAfter;

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
