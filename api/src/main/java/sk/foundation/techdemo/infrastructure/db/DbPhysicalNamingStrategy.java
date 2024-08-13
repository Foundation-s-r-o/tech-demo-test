package sk.foundation.techdemo.infrastructure.db;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class DbPhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

	@Override
	protected boolean isCaseInsensitive(JdbcEnvironment jdbcEnvironment) {
		return false;
	}

}
