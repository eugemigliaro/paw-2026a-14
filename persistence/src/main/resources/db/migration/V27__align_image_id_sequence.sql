CREATE SEQUENCE IF NOT EXISTS images_imageid_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE images
	ALTER COLUMN id SET DEFAULT nextval('images_imageid_seq');

ALTER SEQUENCE images_imageid_seq OWNED BY images.id;

SELECT setval(
	'images_imageid_seq',
	COALESCE((SELECT MAX(id) FROM images), 0) + 1,
	false
);
