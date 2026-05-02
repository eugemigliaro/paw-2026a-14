CREATE TYPE report_target_type AS ENUM ('match', 'review', 'user');

CREATE TYPE report_status AS ENUM ('pending', 'under_review', 'resolved', 'appealed', 'finalized');

CREATE TYPE report_reason AS ENUM (
	'inappropriate_content',
	'aggressive_language',
	'spam',
	'harassment',
	'cheating',
	'other');

CREATE TYPE report_resolution AS ENUM ('dismissed', 'content_deleted', 'user_banned');

CREATE TYPE report_appeal_decision AS ENUM ('upheld', 'lifted');

CREATE TABLE moderation_reports (
	id BIGSERIAL PRIMARY KEY,
	reporter_user_id BIGINT NOT NULL REFERENCES users(id),
	target_type report_target_type NOT NULL,
	target_id BIGINT NOT NULL,
	reason report_reason NOT NULL,
	details TEXT,
	status report_status NOT NULL DEFAULT 'pending',
	resolution report_resolution,
	resolution_details TEXT,
	reviewed_by_user_id BIGINT REFERENCES users(id),
	reviewed_at TIMESTAMPTZ,
	appeal_reason TEXT,
	appeal_count SMALLINT NOT NULL DEFAULT 0 CHECK (appeal_count >= 0 AND appeal_count <= 1),
	appealed_at TIMESTAMPTZ,
	appeal_decision report_appeal_decision,
	appeal_resolved_by_user_id BIGINT REFERENCES users(id),
	appeal_resolved_at TIMESTAMPTZ,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT uq_moderation_reports_reporter_target UNIQUE (reporter_user_id, target_type, target_id)
);

CREATE INDEX idx_moderation_reports_target_type_target_id
	ON moderation_reports(target_type, target_id);

CREATE INDEX idx_moderation_reports_status
	ON moderation_reports(status);

CREATE INDEX idx_moderation_reports_reporter_user_id
	ON moderation_reports(reporter_user_id);


CREATE TABLE user_bans (
	id BIGSERIAL PRIMARY KEY,
	moderation_report_id BIGINT NOT NULL REFERENCES moderation_reports(id),
	banned_until TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_bans_banned_until ON user_bans(banned_until);

ALTER TABLE matches
	ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE matches
	ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE matches
	ADD COLUMN deleted_by_user_id BIGINT REFERENCES users(id);

ALTER TABLE matches
	ADD COLUMN delete_reason TEXT;

CREATE INDEX idx_matches_deleted ON matches(deleted);

ALTER TABLE player_reviews
	ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE player_reviews
	ADD COLUMN deleted_by_user_id BIGINT REFERENCES users(id);

ALTER TABLE player_reviews
	ADD COLUMN delete_reason TEXT;

UPDATE player_reviews SET deleted = TRUE WHERE deleted_at IS NOT NULL;

CREATE INDEX idx_player_reviews_deleted ON player_reviews(deleted);
