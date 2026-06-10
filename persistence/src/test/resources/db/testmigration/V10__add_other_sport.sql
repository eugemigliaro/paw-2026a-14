-- Mirror of prod V10 (ALTER TYPE sport_type ADD VALUE 'other'). Emulated by
-- widening the CHECK.
ALTER TABLE matches DROP CONSTRAINT ck_matches_sport;

ALTER TABLE matches
	ADD CONSTRAINT ck_matches_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball', 'other'));
