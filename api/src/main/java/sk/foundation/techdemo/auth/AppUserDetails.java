package sk.foundation.techdemo.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/**
 * Spring Security principal backed by a {@link User} entity. Carries display fields
 * (first/last name, role) so the authenticated user can be returned without a DB re-read.
 */
@Getter
public class AppUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final String username;
	private final transient String password;
	private final String firstName;
	private final String lastName;
	private final String role;
	private final boolean enabled;

	public AppUserDetails(User user) {
		this.username = user.getUsername();
		this.password = user.getPassword();
		this.firstName = user.getFirstName();
		this.lastName = user.getLastName();
		this.role = user.getRole();
		this.enabled = user.isEnabled();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
