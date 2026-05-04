ALTER TABLE matches
	ADD COLUMN latitude DOUBLE PRECISION;

ALTER TABLE matches
	ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE matches
	ADD CONSTRAINT chk_matches_coordinates_pair
	CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL));

ALTER TABLE matches
	ADD CONSTRAINT chk_matches_latitude_range
	CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90));

ALTER TABLE matches
	ADD CONSTRAINT chk_matches_longitude_range
	CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));

CREATE INDEX idx_matches_coordinates ON matches(latitude, longitude);
