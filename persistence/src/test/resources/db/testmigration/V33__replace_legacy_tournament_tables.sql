-- Mirror of prod V33. Drops the legacy tournament schema and rebuilds it.
-- Prod relies on BIGSERIAL auto-creating the <table>_id_seq sequences; HSQLDB
-- does not, so the named sequences Hibernate expects are created explicitly
-- here. pairing_strategy is intentionally absent (added in V34, matching prod).
DROP TABLE tournament_matches IF EXISTS CASCADE;
DROP TABLE tournament_solo_entries IF EXISTS CASCADE;
DROP TABLE tournament_team_members IF EXISTS CASCADE;
DROP TABLE tournament_teams IF EXISTS CASCADE;
DROP TABLE tournaments IF EXISTS CASCADE;

CREATE SEQUENCE tournaments_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_teams_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_team_members_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_solo_entries_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE tournament_matches_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE tournaments (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournaments_id_seq,
	host_user_id BIGINT NOT NULL REFERENCES users(id),
	sport VARCHAR(30) NOT NULL,
	title VARCHAR(150) NOT NULL,
	description TEXT,
	address VARCHAR(255) NOT NULL,
	latitude DOUBLE,
	longitude DOUBLE,
	starts_at TIMESTAMP,
	ends_at TIMESTAMP,
	price_per_player NUMERIC(10,2) DEFAULT 0 CHECK (price_per_player >= 0),
	banner_image_id BIGINT REFERENCES images(id) ON DELETE SET NULL,
	format VARCHAR(40) NOT NULL DEFAULT 'single_elimination',
	bracket_size SMALLINT NOT NULL,
	team_size SMALLINT NOT NULL,
	allow_solo_signup BOOLEAN NOT NULL DEFAULT TRUE,
	allow_team_draft BOOLEAN NOT NULL DEFAULT FALSE,
	registration_opens_at TIMESTAMP NOT NULL,
	registration_closes_at TIMESTAMP NOT NULL,
	status VARCHAR(40) NOT NULL DEFAULT 'registration',
	registration_closed_at TIMESTAMP,
	bracket_generated_at TIMESTAMP,
	started_at TIMESTAMP,
	completed_at TIMESTAMP,
	cancelled_at TIMESTAMP,
	cancel_reason TEXT,
	deleted BOOLEAN NOT NULL DEFAULT FALSE,
	deleted_at TIMESTAMP,
	deleted_by_user_id BIGINT REFERENCES users(id),
	delete_reason TEXT,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT ck_tournaments_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball', 'other')),
	CONSTRAINT ck_tournaments_format CHECK (format IN ('single_elimination')),
	CONSTRAINT ck_tournaments_bracket_size CHECK (bracket_size IN (4, 8, 16)),
	CONSTRAINT ck_tournaments_team_size CHECK (team_size >= 1),
	CONSTRAINT ck_tournaments_join_mode CHECK (allow_solo_signup OR allow_team_draft),
	CONSTRAINT ck_tournaments_registration_window CHECK (registration_closes_at > registration_opens_at),
	CONSTRAINT ck_tournaments_play_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
	CONSTRAINT ck_tournaments_status CHECK (
		status IN ('registration', 'bracket_setup', 'in_progress', 'completed', 'cancelled')
	),
	CONSTRAINT ck_tournaments_coordinates_pair CHECK (
		(latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
	),
	CONSTRAINT ck_tournaments_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
	CONSTRAINT ck_tournaments_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

CREATE INDEX idx_tournaments_host_user_id ON tournaments(host_user_id);
CREATE INDEX idx_tournaments_status ON tournaments(status);
CREATE INDEX idx_tournaments_sport ON tournaments(sport);
CREATE INDEX idx_tournaments_registration_closes_at ON tournaments(registration_closes_at);
CREATE INDEX idx_tournaments_banner_image_id ON tournaments(banner_image_id);
CREATE INDEX idx_tournaments_deleted ON tournaments(deleted);

CREATE TABLE tournament_teams (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_teams_id_seq,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	name VARCHAR(150) NOT NULL,
	origin VARCHAR(40) NOT NULL,
	seed_position SMALLINT,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT ck_tournament_teams_origin CHECK (origin IN ('solo_pool', 'team_draft')),
	CONSTRAINT ck_tournament_teams_seed_positive CHECK (seed_position IS NULL OR seed_position > 0),
	CONSTRAINT uq_tournament_teams_tournament_name UNIQUE (tournament_id, name)
);

CREATE INDEX idx_tournament_teams_tournament_id ON tournament_teams(tournament_id);
-- Prod uses a partial unique index (... WHERE seed_position IS NOT NULL);
-- HSQLDB's unique index keeps NULL seeds distinct under SQL NULL semantics.
CREATE UNIQUE INDEX uq_tournament_teams_tournament_seed ON tournament_teams(tournament_id, seed_position);

CREATE TABLE tournament_team_members (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_team_members_id_seq,
	team_id BIGINT NOT NULL REFERENCES tournament_teams(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	is_captain BOOLEAN NOT NULL DEFAULT FALSE,
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT uq_tournament_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_tournament_team_members_team_id ON tournament_team_members(team_id);
CREATE INDEX idx_tournament_team_members_user_id ON tournament_team_members(user_id);

CREATE TABLE tournament_solo_entries (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_solo_entries_id_seq,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	user_id BIGINT NOT NULL REFERENCES users(id),
	status VARCHAR(40) NOT NULL DEFAULT 'in_pool',
	assigned_team_id BIGINT REFERENCES tournament_teams(id) ON DELETE SET NULL,
	joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	left_at TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT ck_tournament_solo_entries_status CHECK (
		status IN ('in_pool', 'left', 'assigned', 'unassigned')
	),
	CONSTRAINT ck_tournament_solo_entries_assignment CHECK (
		(status = 'assigned' AND assigned_team_id IS NOT NULL)
		OR (status <> 'assigned' AND assigned_team_id IS NULL)
	),
	CONSTRAINT uq_tournament_solo_entries_tournament_user UNIQUE (tournament_id, user_id)
);

CREATE INDEX idx_tournament_solo_entries_tournament_id ON tournament_solo_entries(tournament_id);
CREATE INDEX idx_tournament_solo_entries_user_id ON tournament_solo_entries(user_id);
CREATE INDEX idx_tournament_solo_entries_tournament_status ON tournament_solo_entries(tournament_id, status);
CREATE INDEX idx_tournament_solo_entries_assigned_team_id ON tournament_solo_entries(assigned_team_id);

CREATE TABLE tournament_matches (
	id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR tournament_matches_id_seq,
	tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
	round_number SMALLINT NOT NULL,
	match_index SMALLINT NOT NULL,
	team_a_id BIGINT REFERENCES tournament_teams(id),
	team_b_id BIGINT REFERENCES tournament_teams(id),
	winner_team_id BIGINT REFERENCES tournament_teams(id),
	scheduled_starts_at TIMESTAMP,
	scheduled_ends_at TIMESTAMP,
	address VARCHAR(255),
	latitude DOUBLE,
	longitude DOUBLE,
	status VARCHAR(40) NOT NULL DEFAULT 'pending',
	parent_match_a_id BIGINT REFERENCES tournament_matches(id),
	parent_match_b_id BIGINT REFERENCES tournament_matches(id),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT uq_tournament_matches_round_index UNIQUE (tournament_id, round_number, match_index),
	CONSTRAINT ck_tournament_matches_round_positive CHECK (round_number > 0),
	CONSTRAINT ck_tournament_matches_index_non_negative CHECK (match_index >= 0),
	CONSTRAINT ck_tournament_matches_distinct_teams CHECK (team_a_id IS NULL OR team_b_id IS NULL OR team_a_id <> team_b_id),
	CONSTRAINT ck_tournament_matches_winner CHECK (
		winner_team_id IS NULL OR winner_team_id = team_a_id OR winner_team_id = team_b_id
	),
	CONSTRAINT ck_tournament_matches_status CHECK (
		status IN ('pending', 'scheduled', 'awaiting_result', 'done', 'walkover')
	),
	CONSTRAINT ck_tournament_matches_schedule_window CHECK (
		scheduled_ends_at IS NULL OR scheduled_starts_at IS NULL OR scheduled_ends_at > scheduled_starts_at
	),
	CONSTRAINT ck_tournament_matches_coordinates_pair CHECK (
		(latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
	),
	CONSTRAINT ck_tournament_matches_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
	CONSTRAINT ck_tournament_matches_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
	CONSTRAINT ck_tournament_matches_distinct_parents CHECK (
		parent_match_a_id IS NULL OR parent_match_b_id IS NULL OR parent_match_a_id <> parent_match_b_id
	)
);

CREATE INDEX idx_tournament_matches_tournament_id ON tournament_matches(tournament_id);
CREATE INDEX idx_tournament_matches_tournament_status ON tournament_matches(tournament_id, status);
CREATE INDEX idx_tournament_matches_team_a_id ON tournament_matches(team_a_id);
CREATE INDEX idx_tournament_matches_team_b_id ON tournament_matches(team_b_id);
CREATE INDEX idx_tournament_matches_winner_team_id ON tournament_matches(winner_team_id);
CREATE INDEX idx_tournament_matches_parent_match_a_id ON tournament_matches(parent_match_a_id);
CREATE INDEX idx_tournament_matches_parent_match_b_id ON tournament_matches(parent_match_b_id);

-- Prod uses partial unique indexes (... WHERE parent_match_*_id IS NOT NULL);
-- HSQLDB's unique index keeps NULL parents distinct under SQL NULL semantics.
CREATE UNIQUE INDEX uq_tournament_matches_parent_a ON tournament_matches(parent_match_a_id);
CREATE UNIQUE INDEX uq_tournament_matches_parent_b ON tournament_matches(parent_match_b_id);
