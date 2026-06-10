-- No-op on the HSQLDB test side.
-- Prod V26 converts every Postgres ENUM column to VARCHAR and adds the named
-- CHECK constraints. The test schema never used enums: each column was created
-- as VARCHAR with the equivalent named CHECK in its originating migration
-- (ck_matches_visibility/status/sport/join_policy, ck_match_participants_status,
-- ck_tournaments_*, ck_email_action_requests_*, ck_match_series_frequency,
-- ck_player_reviews_reaction, ck_moderation_reports_*). The end state already
-- matches, so there is nothing to convert.
VALUES (1);
