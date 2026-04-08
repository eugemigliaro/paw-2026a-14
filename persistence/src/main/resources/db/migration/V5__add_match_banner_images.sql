CREATE TABLE images (
	id BIGSERIAL PRIMARY KEY,
	content_type VARCHAR(100) NOT NULL,
	content_length BIGINT NOT NULL CHECK (content_length > 0),
	content BYTEA NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE matches
	ADD COLUMN banner_image_id BIGINT REFERENCES images(id) ON DELETE SET NULL;

CREATE INDEX idx_matches_banner_image_id ON matches(banner_image_id);
