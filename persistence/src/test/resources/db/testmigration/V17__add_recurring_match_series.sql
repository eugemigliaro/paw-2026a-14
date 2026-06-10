-- Mirror of prod V17. recurrence_frequency_type enum emulated with CHECK.
-- match_series uses BIGSERIAL here; the named match_series_id_seq Hibernate
-- expects is created in V28 (align_match_id_sequences), as in prod.
CREATE TABLE match_series (
	id BIGSERIAL PRIMARY KEY,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	frequency VARCHAR(30) NOT NULL,
	starts_at TIMESTAMP NOT NULL,
	ends_at TIMESTAMP,
	timezone VARCHAR(100) NOT NULL,
	until_date DATE,
	occurrence_count INTEGER,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_match_series_frequency CHECK (frequency IN ('daily', 'weekly', 'monthly')),
	CHECK (ends_at IS NULL OR ends_at > starts_at),
	CHECK (
		(until_date IS NOT NULL AND occurrence_count IS NULL)
		OR (until_date IS NULL AND occurrence_count IS NOT NULL AND occurrence_count >= 2)
	)
);

CREATE INDEX idx_match_series_host_user_id ON match_series(host_user_id);

ALTER TABLE matches
	ADD COLUMN series_id BIGINT REFERENCES match_series(id) ON DELETE SET NULL;

ALTER TABLE matches
	ADD COLUMN series_occurrence_index INTEGER;

ALTER TABLE matches
	ADD CONSTRAINT chk_matches_series_identity_complete CHECK (
		(series_id IS NULL AND series_occurrence_index IS NULL)
		OR (series_id IS NOT NULL AND series_occurrence_index IS NOT NULL)
	);

CREATE INDEX idx_matches_series_id ON matches(series_id);

-- Prod uses a partial unique index (... WHERE series_id IS NOT NULL); HSQLDB
-- enforces the same uniqueness via a unique constraint on the pair (NULL
-- series rows stay distinct under SQL NULL semantics).
ALTER TABLE matches
	ADD CONSTRAINT uq_matches_series_occurrence UNIQUE (series_id, series_occurrence_index);
