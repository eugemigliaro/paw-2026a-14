-- matches_matchid_seq

CREATE SEQUENCE IF NOT EXISTS matches_matchid_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE matches
	ALTER COLUMN id SET DEFAULT nextval('matches_matchid_seq');

ALTER SEQUENCE matches_matchid_seq OWNED BY matches.id;

SELECT setval(
	'matches_matchid_seq',
	COALESCE((SELECT MAX(id) FROM matches), 0) + 1,
	false
);

-- match_series_id_seq

CREATE SEQUENCE IF NOT EXISTS match_series_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE match_series
	ALTER COLUMN id SET DEFAULT nextval('match_series_id_seq');

ALTER SEQUENCE match_series_id_seq OWNED BY match_series.id;

SELECT setval(
	'match_series_id_seq',
	COALESCE((SELECT MAX(id) FROM match_series), 0) + 1,
	false
);
