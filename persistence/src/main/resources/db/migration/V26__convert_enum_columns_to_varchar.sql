ALTER TABLE matches
	ALTER COLUMN visibility DROP DEFAULT,
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN sport DROP DEFAULT,
	ALTER COLUMN join_policy DROP DEFAULT;

ALTER TABLE matches
	ALTER COLUMN visibility TYPE VARCHAR(20) USING visibility::TEXT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN sport TYPE VARCHAR(30) USING sport::TEXT,
	ALTER COLUMN join_policy TYPE VARCHAR(30) USING join_policy::TEXT;

ALTER TABLE matches
	ALTER COLUMN visibility SET DEFAULT 'public',
	ALTER COLUMN status SET DEFAULT 'draft',
	ALTER COLUMN sport SET DEFAULT 'football',
	ALTER COLUMN join_policy SET DEFAULT 'direct';

ALTER TABLE match_participants
	ALTER COLUMN status DROP DEFAULT;

ALTER TABLE match_participants
	ALTER COLUMN status TYPE VARCHAR(30) USING status::TEXT;

ALTER TABLE match_participants
	ALTER COLUMN status SET DEFAULT 'joined';

ALTER TABLE tournaments
	ALTER COLUMN format TYPE VARCHAR(30) USING format::TEXT,
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN status SET DEFAULT 'draft';

ALTER TABLE tournament_teams
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN status SET DEFAULT 'pending';

ALTER TABLE tournament_team_members
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN status SET DEFAULT 'joined';

DROP INDEX IF EXISTS uq_tournament_matches_next_home_slot;
DROP INDEX IF EXISTS uq_tournament_matches_next_away_slot;

ALTER TABLE tournament_matches
	ALTER COLUMN winner_to_slot TYPE VARCHAR(10) USING winner_to_slot::TEXT;

CREATE UNIQUE INDEX uq_tournament_matches_next_home_slot
	ON tournament_matches(next_match_id)
	WHERE winner_to_slot = 'home';

CREATE UNIQUE INDEX uq_tournament_matches_next_away_slot
	ON tournament_matches(next_match_id)
	WHERE winner_to_slot = 'away';

ALTER TABLE email_action_requests
	ALTER COLUMN action_type TYPE VARCHAR(50) USING action_type::TEXT,
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN status SET DEFAULT 'pending';

ALTER TABLE match_series
	ALTER COLUMN frequency TYPE VARCHAR(30) USING frequency::TEXT;

ALTER TABLE player_reviews
	ALTER COLUMN reaction TYPE VARCHAR(20) USING reaction::TEXT;

ALTER TABLE moderation_reports
	ALTER COLUMN target_type TYPE VARCHAR(20) USING target_type::TEXT,
	ALTER COLUMN reason TYPE VARCHAR(30) USING reason::TEXT,
	ALTER COLUMN status DROP DEFAULT,
	ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
	ALTER COLUMN status SET DEFAULT 'pending',
	ALTER COLUMN resolution TYPE VARCHAR(20) USING resolution::TEXT,
	ALTER COLUMN appeal_decision TYPE VARCHAR(20) USING appeal_decision::TEXT;

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_matches_visibility') THEN
		ALTER TABLE matches
			ADD CONSTRAINT ck_matches_visibility CHECK (visibility IN ('public', 'private', 'invite_only'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_matches_status') THEN
		ALTER TABLE matches
			ADD CONSTRAINT ck_matches_status CHECK (status IN ('draft', 'open', 'completed', 'cancelled'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_matches_sport') THEN
		ALTER TABLE matches
			ADD CONSTRAINT ck_matches_sport CHECK (sport IN ('football', 'tennis', 'padel', 'basketball', 'other'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_matches_join_policy') THEN
		ALTER TABLE matches
			ADD CONSTRAINT ck_matches_join_policy CHECK (join_policy IN ('direct', 'approval_required', 'invite_only'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_match_participants_status') THEN
		ALTER TABLE match_participants
			ADD CONSTRAINT ck_match_participants_status
			CHECK (
				status IN (
					'invited',
					'joined',
					'waitlisted',
					'cancelled',
					'checked_in',
					'pending_approval',
					'declined_invite'
				)
			);
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tournaments_format') THEN
		ALTER TABLE tournaments
			ADD CONSTRAINT ck_tournaments_format CHECK (format IN ('knockout', 'league', 'groups_then_knockout'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tournaments_status') THEN
		ALTER TABLE tournaments
			ADD CONSTRAINT ck_tournaments_status CHECK (status IN ('draft', 'open', 'in_progress', 'completed', 'cancelled'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tournament_teams_status') THEN
		ALTER TABLE tournament_teams
			ADD CONSTRAINT ck_tournament_teams_status CHECK (status IN ('pending', 'approved', 'active', 'eliminated', 'withdrawn'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tournament_team_members_status') THEN
		ALTER TABLE tournament_team_members
			ADD CONSTRAINT ck_tournament_team_members_status CHECK (status IN ('invited', 'joined', 'removed'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tournament_matches_winner_to_slot') THEN
		ALTER TABLE tournament_matches
			ADD CONSTRAINT ck_tournament_matches_winner_to_slot CHECK (winner_to_slot IN ('home', 'away'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_email_action_requests_action_type') THEN
		ALTER TABLE email_action_requests
			ADD CONSTRAINT ck_email_action_requests_action_type
			CHECK (action_type IN ('match_reservation', 'match_creation', 'account_verification', 'password_reset'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_email_action_requests_status') THEN
		ALTER TABLE email_action_requests
			ADD CONSTRAINT ck_email_action_requests_status CHECK (status IN ('pending', 'completed', 'failed', 'expired'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_match_series_frequency') THEN
		ALTER TABLE match_series
			ADD CONSTRAINT ck_match_series_frequency CHECK (frequency IN ('daily', 'weekly', 'monthly'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_player_reviews_reaction') THEN
		ALTER TABLE player_reviews
			ADD CONSTRAINT ck_player_reviews_reaction CHECK (reaction IN ('like', 'dislike'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_moderation_reports_target_type') THEN
		ALTER TABLE moderation_reports
			ADD CONSTRAINT ck_moderation_reports_target_type CHECK (target_type IN ('match', 'review', 'user'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_moderation_reports_reason') THEN
		ALTER TABLE moderation_reports
			ADD CONSTRAINT ck_moderation_reports_reason
			CHECK (reason IN ('inappropriate_content', 'aggressive_language', 'spam', 'harassment', 'cheating', 'other'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_moderation_reports_status') THEN
		ALTER TABLE moderation_reports
			ADD CONSTRAINT ck_moderation_reports_status
			CHECK (status IN ('pending', 'under_review', 'resolved', 'appealed', 'finalized'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_moderation_reports_resolution') THEN
		ALTER TABLE moderation_reports
			ADD CONSTRAINT ck_moderation_reports_resolution
			CHECK (resolution IS NULL OR resolution IN ('dismissed', 'content_deleted', 'user_banned'));
	END IF;

	IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_moderation_reports_appeal_decision') THEN
		ALTER TABLE moderation_reports
			ADD CONSTRAINT ck_moderation_reports_appeal_decision
			CHECK (appeal_decision IS NULL OR appeal_decision IN ('upheld', 'lifted'));
	END IF;
END $$;

DROP TYPE IF EXISTS visibility_type;
DROP TYPE IF EXISTS match_status;
DROP TYPE IF EXISTS participant_status;
DROP TYPE IF EXISTS tournament_format;
DROP TYPE IF EXISTS tournament_status;
DROP TYPE IF EXISTS tournament_team_status;
DROP TYPE IF EXISTS tournament_team_member_status;
DROP TYPE IF EXISTS bracket_slot;
DROP TYPE IF EXISTS sport_type;
DROP TYPE IF EXISTS email_action_type;
DROP TYPE IF EXISTS email_action_status;
DROP TYPE IF EXISTS join_policy_type;
DROP TYPE IF EXISTS recurrence_frequency_type;
DROP TYPE IF EXISTS player_review_reaction;
DROP TYPE IF EXISTS report_target_type;
DROP TYPE IF EXISTS report_status;
DROP TYPE IF EXISTS report_reason;
DROP TYPE IF EXISTS report_resolution;
DROP TYPE IF EXISTS report_appeal_decision;
