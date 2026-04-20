ALTER TABLE users
	ADD COLUMN profile_image_id BIGINT REFERENCES images(id) ON DELETE SET NULL;

CREATE INDEX idx_users_profile_image_id ON users(profile_image_id);
