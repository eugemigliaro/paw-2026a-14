-- Mirror of prod V8 (ALTER TYPE participant_status ADD VALUE
-- 'pending_approval'). Emulated by widening the CHECK.
ALTER TABLE match_participants DROP CONSTRAINT ck_match_participants_status;

ALTER TABLE match_participants
	ADD CONSTRAINT ck_match_participants_status CHECK (
		status IN ('invited', 'joined', 'waitlisted', 'cancelled', 'checked_in', 'pending_approval')
	);
