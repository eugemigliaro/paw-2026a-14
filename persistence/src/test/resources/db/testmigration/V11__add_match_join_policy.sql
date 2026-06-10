-- Mirror of prod V11. join_policy is added as a plain VARCHAR with no CHECK
-- yet (prod constrains it only once it becomes an enum in V12). The two data
-- UPDATEs replicate prod exactly.
ALTER TABLE matches
	ADD COLUMN join_policy VARCHAR(30) NOT NULL DEFAULT 'direct';

UPDATE matches
SET join_policy = CASE WHEN visibility = 'public' THEN 'direct' ELSE 'approval_required' END;

UPDATE matches
SET visibility = 'private', join_policy = 'approval_required'
WHERE visibility = 'invite_only';
