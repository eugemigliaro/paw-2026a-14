-- Mirror of prod V32. Prod creates user_sport_ratings_id_seq inline here (not
-- in an align migration), so the test does the same.
CREATE SEQUENCE user_sport_ratings_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE user_sport_ratings (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR user_sport_ratings_id_seq,
	user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	sport VARCHAR(30) NOT NULL,
	elo INTEGER NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT uq_user_sport_ratings_user_sport UNIQUE (user_id, sport),
	CONSTRAINT ck_user_sport_ratings_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball'))
);

CREATE INDEX idx_user_sport_ratings_user_id ON user_sport_ratings(user_id);
-- Prod indexes (sport, elo DESC, user_id); HSQLDB index direction does not
-- affect correctness for these lookups.
CREATE INDEX idx_user_sport_ratings_sport_elo_user_id ON user_sport_ratings(sport, elo, user_id);
