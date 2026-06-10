-- Mirror of prod V7 (ALTER TYPE email_action_type ADD VALUE
-- 'account_verification', 'password_reset'). Emulated by widening the CHECK.
ALTER TABLE email_action_requests DROP CONSTRAINT ck_email_action_requests_action_type;

ALTER TABLE email_action_requests
	ADD CONSTRAINT ck_email_action_requests_action_type
	CHECK (action_type IN ('match_reservation', 'match_creation', 'account_verification', 'password_reset'));
