-- player_reviews_id_seq

CREATE SEQUENCE IF NOT EXISTS player_reviews_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE player_reviews
	ALTER COLUMN id SET DEFAULT nextval('player_reviews_id_seq');

ALTER SEQUENCE player_reviews_id_seq OWNED BY player_reviews.id;

SELECT setval(
	'player_reviews_id_seq',
	COALESCE((SELECT MAX(id) FROM player_reviews), 0) + 1,
	false
);
