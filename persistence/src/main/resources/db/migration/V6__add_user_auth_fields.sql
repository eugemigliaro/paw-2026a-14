ALTER TABLE users
ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'user';

ALTER TABLE users
ADD COLUMN password_hash VARCHAR(255);

ALTER TABLE users
ADD COLUMN email_verified_at TIMESTAMPTZ;

ALTER TABLE users
ADD CONSTRAINT ck_users_role CHECK (role IN ('user', 'admin_mod'));

UPDATE users
SET email_verified_at = created_at
WHERE email_verified_at IS NULL;
