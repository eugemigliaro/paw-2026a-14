CREATE TABLE user_bans (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL REFERENCES users(id),
	banned_by_user_id BIGINT NOT NULL REFERENCES users(id),
	reason TEXT NOT NULL,
	banned_until TIMESTAMP NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
	appeal_reason TEXT,
	appeal_count SMALLINT NOT NULL DEFAULT 0,
	appealed_at TIMESTAMP,
	appeal_resolved_at TIMESTAMP,
	appeal_resolved_by_user_id BIGINT REFERENCES users(id),
	appeal_decision VARCHAR(20)
);

CREATE INDEX idx_user_bans_user_id ON user_bans(user_id);
CREATE INDEX idx_user_bans_banned_until ON user_bans(banned_until);

ALTER TABLE matches
	ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE matches
	ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE matches
	ADD COLUMN deleted_by_user_id BIGINT REFERENCES users(id);

ALTER TABLE matches
	ADD COLUMN delete_reason TEXT;

CREATE INDEX idx_matches_deleted ON matches(deleted);

ALTER TABLE player_reviews
	ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE player_reviews
	ADD COLUMN deleted_by_user_id BIGINT REFERENCES users(id);

ALTER TABLE player_reviews
	ADD COLUMN delete_reason VARCHAR(30);

UPDATE player_reviews
	SET deleted = TRUE
	WHERE deleted_at IS NOT NULL;

CREATE INDEX idx_player_reviews_deleted ON player_reviews(deleted);

CREATE TABLE moderation_reports (
	id BIGSERIAL PRIMARY KEY,
	reporter_user_id BIGINT NOT NULL REFERENCES users(id),
	target_type VARCHAR(20) NOT NULL,
	target_id BIGINT NOT NULL,
	target_key VARCHAR(64) NOT NULL,
	reason VARCHAR(30) NOT NULL,
	details TEXT,
	status VARCHAR(20) NOT NULL DEFAULT 'pending',
	resolution VARCHAR(20),
	resolution_details TEXT,
	reviewed_by_user_id BIGINT REFERENCES users(id),
	reviewed_at TIMESTAMP,
	appeal_reason TEXT,
	appeal_count SMALLINT NOT NULL DEFAULT 0,
	appealed_at TIMESTAMP,
	appeal_resolution VARCHAR(20),
	appeal_resolved_by_user_id BIGINT REFERENCES users(id),
	appeal_resolved_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
	CONSTRAINT uq_moderation_reports_reporter_target UNIQUE (reporter_user_id, target_key)
);

CREATE INDEX idx_moderation_reports_target_type_target_id
	ON moderation_reports(target_type, target_id);

CREATE INDEX idx_moderation_reports_status
	ON moderation_reports(status);

CREATE INDEX idx_moderation_reports_reporter_user_id
	ON moderation_reports(reporter_user_id);
