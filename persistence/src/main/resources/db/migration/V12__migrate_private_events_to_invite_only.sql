UPDATE matches
	SET join_policy = 'invite_only'::join_policy_type
	WHERE visibility = 'private';
