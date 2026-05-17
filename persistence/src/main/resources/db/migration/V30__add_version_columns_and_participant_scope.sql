ALTER TABLE matches ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE match_participants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE match_participants ADD COLUMN scope VARCHAR(20) DEFAULT 'match';

UPDATE match_participants SET scope = CASE WHEN series_request THEN 'series' ELSE 'match' END;

ALTER TABLE match_participants ALTER COLUMN scope SET NOT NULL;
ALTER TABLE match_participants ADD CONSTRAINT chk_match_participants_scope CHECK (scope IN ('match', 'series'));

ALTER TABLE match_participants DROP COLUMN series_request;
