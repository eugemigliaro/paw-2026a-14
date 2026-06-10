-- Mirror of prod V35 (data backfill).
UPDATE tournaments t
SET starts_at = (
		SELECT MIN(tm.scheduled_starts_at)
		FROM tournament_matches tm
		WHERE tm.tournament_id = t.id
			AND tm.scheduled_starts_at IS NOT NULL
	),
	ends_at = (
		SELECT MAX(COALESCE(tm.scheduled_ends_at, tm.scheduled_starts_at))
		FROM tournament_matches tm
		WHERE tm.tournament_id = t.id
			AND tm.scheduled_starts_at IS NOT NULL
	)
WHERE EXISTS (
	SELECT 1
	FROM tournament_matches tm
	WHERE tm.tournament_id = t.id
		AND tm.scheduled_starts_at IS NOT NULL
);
