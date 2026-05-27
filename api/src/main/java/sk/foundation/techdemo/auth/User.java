package sk.foundation.techdemo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;
import sk.foundation.techdemo.infrastructure.db.IdentifiableEntity;

/**
 * Application user for simple DB-backed login. This layer is intentionally minimal and
 * swappable — authentication will move to Keycloak later (see docs/UPGRADE_PLAN.md, Phase 7).
 */
@Getter
@Setter
@Entity
@Table(
		name = "USERS",
		uniqueConstraints = { @UniqueConstraint(name = User.UK_USERNAME, columnNames = "USERNAME") })
public class User extends IdentifiableEntity<Long> {

	private static final long serialVersionUID = 1L;

	public static final String UK_USERNAME = "UK_USERNAME";

	@Column(name = "USERNAME", nullable = false)
	private String username;

	@Column(name = "PASSWORD", nullable = false)
	private String password;

	@Column(name = "FIRST_NAME")
	private String firstName;

	@Column(name = "LAST_NAME")
	private String lastName;

	@Column(name = "ROLE", nullable = false)
	private String role;

	@Column(name = "ENABLED", nullable = false)
	private boolean enabled;

}
