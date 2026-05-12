-- HSQLDB-compatible schema for testing

-- Create sequences for compatibility with JPA
CREATE SEQUENCE users_userid_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE matches_matchid_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE match_participants_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournaments_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_teams_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_team_members_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_brackets_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_matches_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE player_reviews_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE email_action_requests_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE images_imageid_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE moderation_reports_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_bans_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR users_userid_seq,
	username VARCHAR(50) NOT NULL UNIQUE,
	name VARCHAR(150),
	last_name VARCHAR(150),
	email VARCHAR(255) NOT NULL UNIQUE,
	phone VARCHAR(50),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matches (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR matches_matchid_seq,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	address VARCHAR(255) NOT NULL,
	title VARCHAR(150) NOT NULL,
	description CLOB,
	starts_at TIMESTAMP NOT NULL,
	ends_at TIMESTAMP,
	max_players INTEGER NOT NULL CHECK (max_players > 0),
	price_per_player NUMERIC(10,2) DEFAULT 0,
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

CREATE TABLE match_participants (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR match_participants_id_seq,
	match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	status VARCHAR(20) NOT NULL DEFAULT 'joined',
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_match_participants_status CHECK (
		status IN (
			'invited',
			'joined',
			'waitlisted',
			'cancelled',
			'checked_in',
			'pending_approval',
			'declined_invite'
		)
	),
	UNIQUE (match_id, user_id)
);

CREATE INDEX idx_match_participants_match_id ON match_participants(match_id);
CREATE INDEX idx_match_participants_user_id ON match_participants(user_id);

CREATE TABLE tournaments (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournaments_id_seq,
	name VARCHAR(150) NOT NULL,
	description CLOB,
	address VARCHAR(255) NOT NULL,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	format VARCHAR(30) NOT NULL,
	status VARCHAR(20) NOT NULL DEFAULT 'draft',
	starts_at TIMESTAMP,
	ends_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournaments_format CHECK (format IN ('knockout', 'league', 'groups_then_knockout')),
	CONSTRAINT ck_tournaments_status CHECK (status IN ('draft', 'open', 'in_progress', 'completed', 'cancelled'))
);

CREATE INDEX idx_tournaments_host_user_id ON tournaments(host_user_id);
CREATE INDEX idx_tournaments_status ON tournaments(status);

CREATE TABLE tournament_teams (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_teams_id_seq,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	name VARCHAR(150) NOT NULL,
	status VARCHAR(20) NOT NULL DEFAULT 'pending',
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_teams_status CHECK (status IN ('pending', 'approved', 'active', 'eliminated', 'withdrawn')),
	UNIQUE (tournament_id, name)
);

CREATE INDEX idx_tournament_teams_tournament_id ON tournament_teams(tournament_id);

CREATE TABLE tournament_team_members (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_team_members_id_seq,
	tournament_team_id BIGINT NOT NULL REFERENCES tournament_teams(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	status VARCHAR(20) NOT NULL DEFAULT 'joined',
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_team_members_status CHECK (status IN ('invited', 'joined', 'removed')),
	UNIQUE (tournament_team_id, user_id)
);

CREATE INDEX idx_tournament_team_members_tournament_team_id ON tournament_team_members(tournament_team_id);
CREATE INDEX idx_tournament_team_members_user_id ON tournament_team_members(user_id);

CREATE TABLE tournament_matches (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_matches_id_seq,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	match_id BIGINT NOT NULL REFERENCES matches(id),
	round_number INTEGER NOT NULL CHECK (round_number > 0),
	home_team_id BIGINT REFERENCES tournament_teams(id),
	away_team_id BIGINT REFERENCES tournament_teams(id),
	home_score INTEGER CHECK (home_score IS NULL OR home_score >= 0),
	away_score INTEGER CHECK (away_score IS NULL OR away_score >= 0),
	winner_team_id BIGINT REFERENCES tournament_teams(id),
	next_match_id BIGINT REFERENCES tournament_matches(id),
	winner_to_slot VARCHAR(10),
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT ck_tournament_matches_winner_to_slot CHECK (winner_to_slot IN ('home', 'away'))
);

CREATE INDEX idx_tournament_matches_tournament_id ON tournament_matches(tournament_id);
CREATE INDEX idx_tournament_matches_round_number ON tournament_matches(tournament_id, round_number);
CREATE INDEX idx_tournament_matches_home_team_id ON tournament_matches(home_team_id);
CREATE INDEX idx_tournament_matches_away_team_id ON tournament_matches(away_team_id);
CREATE INDEX idx_tournament_matches_next_match_id ON tournament_matches(next_match_id);

CREATE UNIQUE INDEX uq_tournament_matches_next_home_slot ON tournament_matches(next_match_id, winner_to_slot);

CREATE UNIQUE INDEX uq_tournament_matches_next_away_slot ON tournament_matches(next_match_id, winner_to_slot);
