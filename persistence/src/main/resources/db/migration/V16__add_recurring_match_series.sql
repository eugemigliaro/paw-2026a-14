CREATE TYPE recurrence_frequency_type AS ENUM ('daily', 'weekly', 'monthly');

CREATE TABLE match_series (
	id BIGSERIAL PRIMARY KEY,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	frequency recurrence_frequency_type NOT NULL,
	starts_at TIMESTAMPTZ NOT NULL,
	ends_at TIMESTAMPTZ,
	timezone VARCHAR(100) NOT NULL,
	until_date DATE,
	occurrence_count INTEGER,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CHECK (ends_at IS NULL OR ends_at > starts_at),
	CHECK (
		(until_date IS NOT NULL AND occurrence_count IS NULL)
		OR (until_date IS NULL AND occurrence_count IS NOT NULL AND occurrence_count >= 2)
	)
);

CREATE INDEX idx_match_series_host_user_id ON match_series(host_user_id);

ALTER TABLE matches
	ADD COLUMN series_id BIGINT REFERENCES match_series(id) ON DELETE SET NULL,
	ADD COLUMN series_occurrence_index INTEGER;

ALTER TABLE matches
	ADD CONSTRAINT chk_matches_series_identity_complete
	CHECK (
		(series_id IS NULL AND series_occurrence_index IS NULL)
		OR (series_id IS NOT NULL AND series_occurrence_index IS NOT NULL)
	);

CREATE INDEX idx_matches_series_id ON matches(series_id);

CREATE UNIQUE INDEX uq_matches_series_occurrence
ON matches(series_id, series_occurrence_index)
WHERE series_id IS NOT NULL;
