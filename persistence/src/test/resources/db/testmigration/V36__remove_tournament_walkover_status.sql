-- Mirror of prod V36.
UPDATE tournament_matches
SET status = 'done'
WHERE status = 'walkover';

ALTER TABLE tournament_matches
	DROP CONSTRAINT ck_tournament_matches_status;

ALTER TABLE tournament_matches
	ADD CONSTRAINT ck_tournament_matches_status CHECK (
		status IN ('pending', 'scheduled', 'awaiting_result', 'done')
	);
