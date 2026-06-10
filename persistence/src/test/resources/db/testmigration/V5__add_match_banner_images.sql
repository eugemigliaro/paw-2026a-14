-- Mirror of prod V5. Postgres BYTEA becomes HSQLDB LONGVARBINARY.
CREATE TABLE images (
	id BIGSERIAL PRIMARY KEY,
	content_type VARCHAR(100) NOT NULL,
	content_length BIGINT NOT NULL CHECK (content_length > 0),
	content LONGVARBINARY NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE matches
	ADD COLUMN banner_image_id BIGINT;

ALTER TABLE matches
	ADD CONSTRAINT fk_matches_banner_image
	FOREIGN KEY (banner_image_id) REFERENCES images(id) ON DELETE SET NULL;

CREATE INDEX idx_matches_banner_image_id ON matches(banner_image_id);
