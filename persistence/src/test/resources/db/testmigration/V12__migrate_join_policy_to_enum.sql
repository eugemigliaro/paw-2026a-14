-- Mirror of prod V12. Prod converts join_policy into the join_policy_type enum
-- ('direct', 'approval_required'). With no enums in HSQLDB, the equivalent
-- effect is the named CHECK constraint that enforces the same value set.
ALTER TABLE matches
	ADD CONSTRAINT ck_matches_join_policy CHECK (join_policy IN ('direct', 'approval_required'));
