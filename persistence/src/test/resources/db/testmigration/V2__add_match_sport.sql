-- Mirror of prod V2. Prod adds the sport_type enum with its initial values;
-- the 'other' value is added later (prod V10), so it is NOT included here.
ALTER TABLE matches
	ADD COLUMN sport VARCHAR(30) NOT NULL DEFAULT 'football';

ALTER TABLE matches
	ADD CONSTRAINT ck_matches_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball'));

CREATE INDEX idx_matches_sport ON matches(sport);
