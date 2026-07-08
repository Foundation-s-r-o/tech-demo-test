package sk.foundation.techdemo.records;

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
 * Cause of death (REGISTRY.DEATH_MEDICAL) — health data (GDPR Art. 9), isolated from the
 * administrative death record so grants/scopes can differ. PK is the death record id.
 */
@Getter
@Setter
@DynamicInsert
@Entity
@Table(schema = "REGISTRY", name = "DEATH_MEDICAL")
public class DeathMedical extends NaturalKeyEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "DEATH_RECORD_ID", updatable = false)
	private String deathRecordId;

	@Column(name = "CAUSE_OF_DEATH_ICD10")
	private String causeOfDeathIcd10;

	@Column(name = "AUTOPSY_PERFORMED")
	private Boolean autopsyPerformed;

	@Column(name = "CERTIFIED_BY")
	private String certifiedBy;

	@Column(name = "RECORDED_AT", nullable = false)
	private LocalDateTime recordedAt;

	@Column(name = "RECORDED_BY", nullable = false)
	private String recordedBy;

	@Override
	public String getId() {
		return deathRecordId;
	}
}
