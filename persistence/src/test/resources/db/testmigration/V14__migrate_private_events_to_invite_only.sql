-- Mirror of prod V14 (data migration).
UPDATE matches
SET join_policy = 'invite_only'
WHERE visibility = 'private';
