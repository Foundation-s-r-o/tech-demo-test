package sk.foundation.techdemo.auditlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.NaturalKeyEntity;

/**
 * Hash-chain head (AUDIT.CHAIN_HEAD) — the single row (id = 1) holding the last chain hash.
 * Serialized via a pessimistic lock in {@code AuditService}.
 */
@Getter
@Setter
@Entity
@Table(schema = "AUDIT", name = "CHAIN_HEAD")
public class ChainHead extends NaturalKeyEntity<Integer> {

	private static final long serialVersionUID = 1L;

	public static final int SINGLETON_ID = 1;

	@Id
	@Column(name = "ID", updatable = false)
	private Integer id;

	@Column(name = "LAST_HASH", nullable = false)
	private String lastHash;

	@Override
	public Integer getId() {
		return id;
	}
}
