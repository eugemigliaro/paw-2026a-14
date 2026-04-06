ALTER TABLE email_action_requests DROP CONSTRAINT ck_email_action_type;

ALTER TABLE email_action_requests
ADD CONSTRAINT ck_email_action_type CHECK (action_type IN ('match_reservation', 'match_creation'));
