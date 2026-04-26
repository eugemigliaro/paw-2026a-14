CREATE TYPE player_review_reaction AS ENUM ('like', 'dislike');

CREATE TABLE player_reviews (
	id BIGSERIAL PRIMARY KEY,
	reviewer_user_id BIGINT NOT NULL REFERENCES users(id),
	reviewed_user_id BIGINT NOT NULL REFERENCES users(id),
	origin_match_id BIGINT NOT NULL REFERENCES matches(id),
	reaction player_review_reaction NOT NULL,
	comment TEXT,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	deleted_at TIMESTAMPTZ,
	UNIQUE (reviewer_user_id, reviewed_user_id),
	CHECK (reviewer_user_id <> reviewed_user_id)
);

CREATE INDEX idx_player_reviews_reviewed_active
ON player_reviews(reviewed_user_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_player_reviews_reviewer_user_id
ON player_reviews(reviewer_user_id);

CREATE INDEX idx_player_reviews_origin_match_id
ON player_reviews(origin_match_id);
