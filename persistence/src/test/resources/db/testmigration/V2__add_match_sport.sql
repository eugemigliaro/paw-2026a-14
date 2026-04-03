ALTER TABLE matches
ADD COLUMN sport VARCHAR(30) NOT NULL DEFAULT 'football';

CREATE INDEX idx_matches_sport ON matches(sport);
