package sk.foundation.techdemo.infrastructure.db;

import java.io.Serializable;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;
import org.springframework.data.util.ProxyUtils;

/**
 * Base for entities with an application-assigned natural key (person UID, generated UUID,
 * codelist code) rather than a database identity column. Spring Data cannot infer
 * {@link #isNew()} from a null id here, so a transient flag tracks it: an entity is new until
 * it is persisted or loaded. This keeps {@code save()} doing an INSERT for freshly built
 * aggregates and a MERGE for reattached ones.
 */
public abstract class NaturalKeyEntity<K extends Serializable> implements Persistable<K>, Serializable {

	private static final long serialVersionUID = 1L;

	@Transient
	private boolean persisted = false;

	@Override
	public boolean isNew() {
		return !persisted;
	}

	@PostPersist
	@PostLoad
	void markPersisted() {
		this.persisted = true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !getClass().equals(ProxyUtils.getUserClass(obj))) {
			return false;
		}
		NaturalKeyEntity<?> that = (NaturalKeyEntity<?>) obj;
		return getId() != null && getId().equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getId() == null ? 0 : getId().hashCode();
	}

	@Override
	public String toString() {
		return String.format("Entity of type %s with id: %s", getClass().getName(), getId());
	}
}
