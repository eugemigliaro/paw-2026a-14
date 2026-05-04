ALTER TABLE users
ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';

UPDATE users
SET preferred_language = 'en'
WHERE preferred_language IS NULL OR preferred_language = '';
