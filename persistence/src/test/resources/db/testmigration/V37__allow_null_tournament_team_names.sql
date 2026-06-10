-- Mirror of prod V37.
ALTER TABLE tournament_teams ALTER COLUMN name DROP NOT NULL;

UPDATE tournament_teams
SET name = NULL
WHERE origin = 'solo_pool';
