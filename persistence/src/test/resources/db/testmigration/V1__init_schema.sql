-- HSQLDB-compatible mirror of prod V1__init_schema.sql.
-- Postgres ENUM types are emulated with VARCHAR + named CHECK constraints,
-- carrying ONLY the values present at V1 (later values are added in their own
-- migrations, just as prod widens the enums). BIGSERIAL ids match prod; the
-- named id sequences Hibernate expects are created in the matching align_*
-- migrations (V23-V25, V27-V29, V31), exactly where prod creates them.

-- USERS
CREATE TABLE users (
	id BIGSERIAL PRIMARY KEY,
	username VARCHAR(50) NOT NULL UNIQUE,
	name VARCHAR(150),
	last_name VARCHAR(150),
	email VARCHAR(255) NOT NULL UNIQUE,
	phone VARCHAR(50),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- MATCHES
CREATE TABLE matches (
	id BIGSERIAL PRIMARY KEY,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	address VARCHAR(255) NOT NULL,
	title VARCHAR(150) NOT NULL,
	description TEXT,
	starts_at TIMESTAMP NOT NULL,
	ends_at TIMESTAMP,
	max_players INTEGER NOT NULL CHECK (max_players > 0),
	price_per_player NUMERIC(10,2) DEFAULT 0 CHECK (price_per_player >= 0),
	visibility VARCHAR(20) NOT NULL DEFAULT 'public',
	status VARCHAR(20) NOT NULL DEFAULT 'draft',
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_matches_visibility CHECK (visibility IN ('public', 'private', 'invite_only')),
	CONSTRAINT ck_matches_status CHECK (status IN ('draft', 'open', 'completed', 'cancelled')),
	CHECK (ends_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX idx_matches_host_user_id ON matches(host_user_id);
CREATE INDEX idx_matches_starts_at ON matches(starts_at);
CREATE INDEX idx_matches_status ON matches(status);

-- MATCH PARTICIPANTS
CREATE TABLE match_participants (
	id BIGSERIAL PRIMARY KEY,
	match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	status VARCHAR(30) NOT NULL DEFAULT 'joined',
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_match_participants_status CHECK (
		status IN ('invited', 'joined', 'waitlisted', 'cancelled', 'checked_in')
	),
	UNIQUE (match_id, user_id)
);

CREATE INDEX idx_match_participants_match_id ON match_participants(match_id);
CREATE INDEX idx_match_participants_user_id ON match_participants(user_id);

-- TOURNAMENTS
CREATE TABLE tournaments (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(150) NOT NULL,
	description TEXT,
	address VARCHAR(255) NOT NULL,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	format VARCHAR(30) NOT NULL,
	status VARCHAR(20) NOT NULL DEFAULT 'draft',
	starts_at TIMESTAMP,
	ends_at TIMESTAMP,
	max_teams INTEGER CHECK (max_teams IS NULL OR max_teams > 1),
	max_team_size INTEGER CHECK (max_team_size IS NULL OR max_team_size > 0),
	min_team_size INTEGER CHECK (min_team_size IS NULL OR min_team_size > 0),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournaments_format CHECK (format IN ('knockout', 'league', 'groups_then_knockout')),
	CONSTRAINT ck_tournaments_status CHECK (status IN ('draft', 'open', 'in_progress', 'completed', 'cancelled')),
	CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
	CHECK (
		(max_team_size IS NULL AND min_team_size IS NULL)
		OR (max_team_size IS NOT NULL AND min_team_size IS NOT NULL AND max_team_size >= min_team_size)
	)
);

CREATE INDEX idx_tournaments_host_user_id ON tournaments(host_user_id);
CREATE INDEX idx_tournaments_status ON tournaments(status);

-- TOURNAMENT TEAMS
CREATE TABLE tournament_teams (
	id BIGSERIAL PRIMARY KEY,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	name VARCHAR(150) NOT NULL,
	status VARCHAR(20) NOT NULL DEFAULT 'pending',
	seed INTEGER,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_teams_status CHECK (status IN ('pending', 'approved', 'active', 'eliminated', 'withdrawn')),
	UNIQUE (tournament_id, name)
);

CREATE INDEX idx_tournament_teams_tournament_id ON tournament_teams(tournament_id);

-- TOURNAMENT TEAM MEMBERS
CREATE TABLE tournament_team_members (
	id BIGSERIAL PRIMARY KEY,
	tournament_team_id BIGINT NOT NULL REFERENCES tournament_teams(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	status VARCHAR(20) NOT NULL DEFAULT 'joined',
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_team_members_status CHECK (status IN ('invited', 'joined', 'removed')),
	UNIQUE (tournament_team_id, user_id)
);

CREATE INDEX idx_tournament_team_members_team_id ON tournament_team_members(tournament_team_id);
CREATE INDEX idx_tournament_team_members_user_id ON tournament_team_members(user_id);

-- TOURNAMENT MATCHES / FIXTURES
CREATE TABLE tournament_matches (
	id BIGSERIAL PRIMARY KEY,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	match_id BIGINT NOT NULL REFERENCES matches(id),
	CONSTRAINT uq_tournament_matches_match UNIQUE (match_id),
	round_number INTEGER NOT NULL CHECK (round_number > 0),
	home_team_id BIGINT REFERENCES tournament_teams(id),
	away_team_id BIGINT REFERENCES tournament_teams(id),
	home_score INTEGER CHECK (home_score IS NULL OR home_score >= 0),
	away_score INTEGER CHECK (away_score IS NULL OR away_score >= 0),
	winner_team_id BIGINT REFERENCES tournament_teams(id),
	next_match_id BIGINT REFERENCES tournament_matches(id),
	winner_to_slot VARCHAR(10),
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_matches_winner_to_slot CHECK (winner_to_slot IS NULL OR winner_to_slot IN ('home', 'away')),
	CHECK (home_team_id IS NULL OR away_team_id IS NULL OR home_team_id <> away_team_id),
	CHECK (winner_team_id IS NULL OR winner_team_id = home_team_id OR winner_team_id = away_team_id),
	CHECK (
		(next_match_id IS NULL AND winner_to_slot IS NULL)
		OR (next_match_id IS NOT NULL AND winner_to_slot IS NOT NULL)
	)
);

CREATE INDEX idx_tournament_matches_tournament_id ON tournament_matches(tournament_id);
CREATE INDEX idx_tournament_matches_round_number ON tournament_matches(tournament_id, round_number);
CREATE INDEX idx_tournament_matches_home_team_id ON tournament_matches(home_team_id);
CREATE INDEX idx_tournament_matches_away_team_id ON tournament_matches(away_team_id);
CREATE INDEX idx_tournament_matches_next_match_id ON tournament_matches(next_match_id);

-- Prod uses partial unique indexes (... WHERE winner_to_slot = 'home'/'away').
-- HSQLDB has no partial indexes; a unique index on the (next_match_id,
-- winner_to_slot) pair enforces the same "one home / one away winner feed per
-- target match" rule.
CREATE UNIQUE INDEX uq_tournament_matches_next_home_slot ON tournament_matches(next_match_id, winner_to_slot);
CREATE UNIQUE INDEX uq_tournament_matches_next_away_slot ON tournament_matches(next_match_id, winner_to_slot);
