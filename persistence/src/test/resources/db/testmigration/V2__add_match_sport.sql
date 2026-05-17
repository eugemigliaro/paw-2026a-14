ALTER TABLE matches
ADD COLUMN sport VARCHAR(30) NOT NULL DEFAULT 'football';

ALTER TABLE matches
ADD CONSTRAINT ck_matches_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball', 'other'));

CREATE INDEX idx_matches_sport ON matches(sport);
