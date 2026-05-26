-- user_bans_id_seq

CREATE SEQUENCE IF NOT EXISTS user_bans_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE user_bans
	ALTER COLUMN id SET DEFAULT nextval('user_bans_id_seq');

ALTER SEQUENCE user_bans_id_seq OWNED BY user_bans.id;

SELECT setval(
	'user_bans_id_seq',
	COALESCE((SELECT MAX(id) FROM user_bans), 0) + 1,
	false
);

-- moderation_reports_id_seq

CREATE SEQUENCE IF NOT EXISTS moderation_reports_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE moderation_reports
	ALTER COLUMN id SET DEFAULT nextval('moderation_reports_id_seq');

ALTER SEQUENCE moderation_reports_id_seq OWNED BY moderation_reports.id;

SELECT setval(
	'moderation_reports_id_seq',
	COALESCE((SELECT MAX(id) FROM moderation_reports), 0) + 1,
	false
);
