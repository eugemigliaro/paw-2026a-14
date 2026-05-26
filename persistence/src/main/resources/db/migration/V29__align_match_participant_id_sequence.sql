-- match_participants_id_seq

CREATE SEQUENCE IF NOT EXISTS match_participants_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE match_participants
	ALTER COLUMN id SET DEFAULT nextval('match_participants_id_seq');

ALTER SEQUENCE match_participants_id_seq OWNED BY match_participants.id;

SELECT setval(
	'match_participants_id_seq',
	COALESCE((SELECT MAX(id) FROM match_participants), 0) + 1,
	false
);
