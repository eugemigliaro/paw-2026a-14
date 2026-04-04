CREATE TYPE sport_type AS ENUM ('football', 'tennis', 'padel', 'basketball');

ALTER TABLE matches
ADD COLUMN sport sport_type NOT NULL DEFAULT 'football';

CREATE INDEX idx_matches_sport ON matches(sport);
