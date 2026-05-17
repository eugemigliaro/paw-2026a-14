CREATE SEQUENCE IF NOT EXISTS email_action_requests_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE email_action_requests
	ALTER COLUMN id SET DEFAULT nextval('email_action_requests_id_seq');

ALTER SEQUENCE email_action_requests_id_seq OWNED BY email_action_requests.id;

SELECT setval(
	'email_action_requests_id_seq',
	COALESCE((SELECT MAX(id) FROM email_action_requests), 0) + 1,
	false
);
