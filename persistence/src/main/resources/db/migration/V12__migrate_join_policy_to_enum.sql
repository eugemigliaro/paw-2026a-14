CREATE TYPE join_policy_type AS ENUM ('direct', 'approval_required');

ALTER TABLE matches
	ALTER COLUMN join_policy DROP DEFAULT;

ALTER TABLE matches
	ALTER COLUMN join_policy TYPE join_policy_type
	USING join_policy::join_policy_type;

ALTER TABLE matches
	ALTER COLUMN join_policy SET DEFAULT 'direct'::join_policy_type;
