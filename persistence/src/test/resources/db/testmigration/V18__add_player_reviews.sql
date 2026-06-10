-- Mirror of prod V18. player_review_reaction enum emulated with CHECK.
-- BIGSERIAL id; the named player_reviews_id_seq is created in V31
-- (align_player_review_id_sequence), as in prod.
CREATE TABLE player_reviews (
	id BIGSERIAL PRIMARY KEY,
	reviewer_user_id BIGINT NOT NULL REFERENCES users(id),
	reviewed_user_id BIGINT NOT NULL REFERENCES users(id),
	reaction VARCHAR(20) NOT NULL,
	comment TEXT,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	deleted_at TIMESTAMP,
	CONSTRAINT ck_player_reviews_reaction CHECK (reaction IN ('like', 'dislike')),
	UNIQUE (reviewer_user_id, reviewed_user_id),
	CHECK (reviewer_user_id <> reviewed_user_id)
);

-- Prod uses a partial index (... WHERE deleted_at IS NULL); HSQLDB indexes the
-- (reviewed_user_id, deleted_at) pair to keep active-review lookups indexed.
CREATE INDEX idx_player_reviews_reviewed_active ON player_reviews(reviewed_user_id, deleted_at);

CREATE INDEX idx_player_reviews_reviewer_user_id ON player_reviews(reviewer_user_id);
