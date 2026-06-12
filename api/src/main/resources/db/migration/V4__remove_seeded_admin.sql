-- The known admin/admin account is test data, not a deployable bootstrap credential.
-- LocalAdminInitializer recreates it only for the explicit local and integration-test profiles.
delete from USERS where USERNAME = 'admin';
