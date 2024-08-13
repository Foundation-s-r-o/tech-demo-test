package sk.foundation.techdemo.infrastructure.db;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Adjusted version of AbstractPersistable
 * @see org.springframework.data.jpa.domain.AbstractPersistable
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class })
public abstract class IdentifiableEntity<K extends Serializable> implements Serializable, Persistable<K> {

	private static final long serialVersionUID = 1L;

	@Id
	@Nullable
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", updatable = false)
	private K id;

	/**
	 * Must be {@link Transient} in order to ensure that no JPA provider complains because of a missing setter.
	 *
	 * @see org.springframework.data.domain.Persistable#isNew()
	 */
	@Transient // DATAJPA-622
	@Override
	public boolean isNew() {
		return null == getId();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Entity of type %s with id: %s", this.getClass().getName(), getId());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (null == obj) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!getClass().equals(ProxyUtils.getUserClass(obj))) {
			return false;
		}

		IdentifiableEntity<?> that = (IdentifiableEntity<?>) obj;

		return null == this.getId() ? false : this.getId().equals(that.getId());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int hashCode = 17;

		hashCode += (null == getId() ? 0 : getId().hashCode() * 31);

		return hashCode;
	}

}
