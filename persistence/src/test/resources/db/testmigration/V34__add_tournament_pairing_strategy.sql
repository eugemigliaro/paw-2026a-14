-- Mirror of prod V34.
ALTER TABLE tournaments
	ADD COLUMN pairing_strategy VARCHAR(30);

ALTER TABLE tournaments
	ADD CONSTRAINT ck_tournaments_pairing_strategy
	CHECK (pairing_strategy IS NULL OR pairing_strategy IN ('manual', 'random', 'elo'));
