CREATE SEQUENCE IF NOT EXISTS users_userid_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE users
	ALTER COLUMN id SET DEFAULT nextval('users_userid_seq');

ALTER SEQUENCE users_userid_seq OWNED BY users.id;

SELECT setval(
	'users_userid_seq',
	COALESCE((SELECT MAX(id) FROM users), 0) + 1,
	false
);
