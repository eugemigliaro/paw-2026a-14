-- Mirror of prod V13 (ALTER TYPE join_policy_type ADD VALUE 'invite_only').
-- Emulated by widening the CHECK so the V14 data migration can set it.
ALTER TABLE matches DROP CONSTRAINT ck_matches_join_policy;

ALTER TABLE matches
	ADD CONSTRAINT ck_matches_join_policy CHECK (join_policy IN ('direct', 'approval_required', 'invite_only'));
