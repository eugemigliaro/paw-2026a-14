ALTER TABLE matches
	ADD COLUMN join_policy VARCHAR(30) NOT NULL DEFAULT 'direct';

UPDATE matches
SET join_policy = CASE
	WHEN visibility = 'public' THEN 'direct'
	WHEN visibility = 'private' THEN 'invite_only'
	ELSE 'approval_required'
END;

UPDATE matches
SET visibility = 'private', join_policy = 'invite_only'
WHERE visibility = 'invite_only';

ALTER TABLE matches
	ADD CONSTRAINT ck_matches_join_policy
	CHECK (join_policy IN ('direct', 'approval_required', 'invite_only'));
