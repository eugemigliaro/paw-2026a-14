-- Mirror of prod V20.
ALTER TABLE match_participants
	ADD COLUMN series_request BOOLEAN NOT NULL DEFAULT FALSE;
