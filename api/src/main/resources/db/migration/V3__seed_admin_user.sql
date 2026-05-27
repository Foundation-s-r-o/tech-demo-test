-- Bootstrap demo user: admin / admin (BCrypt hash, strength 10).
-- Deliberate local-demo credential; it goes away when Keycloak replaces this layer.
insert into USERS (USERNAME, PASSWORD, FIRST_NAME, LAST_NAME, ROLE, ENABLED) values
	('admin', '$2a$10$PaP23gYWc3dKqvhHn2gQgOVOnplGoUDEgtsIylBiAccA7PZiymj/i', 'Admin', 'Admin', 'ADMIN', true);
