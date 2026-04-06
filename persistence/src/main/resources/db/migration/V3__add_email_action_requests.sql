CREATE TYPE email_action_type AS ENUM ('match_reservation');

CREATE TYPE email_action_status AS ENUM ('pending', 'completed', 'failed', 'expired');

CREATE TABLE email_action_requests (
	id BIGSERIAL PRIMARY KEY,
	action_type email_action_type NOT NULL,
	email VARCHAR(255) NOT NULL,
	user_id BIGINT REFERENCES users(id),
	token_hash VARCHAR(128) NOT NULL UNIQUE,
	payload_json TEXT NOT NULL,
	status email_action_status NOT NULL DEFAULT 'pending',
	expires_at TIMESTAMPTZ NOT NULL,
	consumed_at TIMESTAMPTZ,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_action_requests_email ON email_action_requests(email);
CREATE INDEX idx_email_action_requests_status ON email_action_requests(status);
