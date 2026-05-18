# Tournaments - Notifications

> Implementation note: this catalog is the long-term target. The first
> tournament spine should implement only the minimum notifications listed in
> [`feature-brief.md`](./feature-brief.md), then expand toward this catalog.
> The current application has email notifications, but no persisted in-app
> notification center or bell inbox.

The platform currently has an email notification pattern through
`MatchNotificationService`, `ThymeleafMailTemplateRenderer`, and
`MailDispatchService`. Tournaments should follow that email pattern first.

Do not assume dual-channel delivery exists. The `email_action_requests` table is
for token/action flows, not a generic notification inbox. Persisted in-app
notifications require a separate foundation: notification table, recipient
queries, unread counts, header UI, mark-read behavior, and authorization.

## Email-First Principle

For every tournament notification implemented in the first spine:

1. The system dispatches a localized templated email to the recipient.
2. The email contains the relevant information and a call-to-action.
3. The CTA opens the in-app page where the user can view or act.

Accept/decline actions, when team drafts are added, happen in the application
after the user follows the email CTA. There is no email-link confirmation flow.

## First-Spine Email Set

Implement only these first:

| Event | Recipient | Email/body copy hint | Subject hint |
| --- | --- | --- | --- |
| Bracket generated and round one scheduled | All assigned tournament players | "Spring Cup bracket is live. Your first match: today at 18:00 vs FC Pampa." | "Your first tournament match is scheduled" |
| Tournament under-capacity cancelled | All registered players | "Spring Cup was cancelled because there were not enough teams." | "Spring Cup has been cancelled" |
| Winner declared - you won | Winning team's players | "You beat FC Pampa. Next: vs Norte United." | "You advanced to the semi-finals" |
| Winner declared - you lost | Losing team's players | "FC Pampa took it. Spring Cup keeps going." | "Your tournament run has ended" |
| Walkover recorded against you | Forfeiting team's players | "A walkover was recorded for your quarter-final." | "Walkover recorded" |
| Tournament completed - champion | Champion team's players | "Your team won Spring Cup." | "You won Spring Cup" |
| Tournament completed - participant | Players who lost earlier | "Spring Cup is over. Los Galacticos took it." | "Spring Cup has concluded" |
| Tournament cancelled mid-play | All remaining players | "Spring Cup was cancelled by the host." | "Tournament cancelled" |

## Expanded Catalog

Add these after the first spine is stable:

### Registration Phase

| Event | Recipient | Email/body copy hint | Subject hint |
| --- | --- | --- | --- |
| Player joined solo pool | Host | "Mariana joined Spring Cup as a solo player." | "New solo sign-up for Spring Cup" |
| Player left solo pool | Host | "Mariana left the solo pool for Spring Cup." | "Solo sign-up withdrew from Spring Cup" |
| Team-draft invitation sent | Invited user | "Diego invited you to Los Galacticos for Spring Cup." | "Diego invited you to Los Galacticos" |
| Team-draft invite accepted | Captain | "Mariana accepted your invite to Los Galacticos." | "Mariana joined your team" |
| Team-draft invite declined | Captain | "Mariana declined your invite to Los Galacticos." | "Mariana declined your team invite" |
| Team locked in | All team members | "Los Galacticos is in the bracket." | "Your team has secured a tournament slot" |
| Team-draft disbanded by captain | All invited/accepted members | "Diego disbanded Los Galacticos." | "Team draft cancelled" |
| Registration closes within 24h | Players in solo pool and open drafts | "Spring Cup registration closes tomorrow." | "Last day to lock in your team" |

### In Progress

| Event | Recipient | Email/body copy hint | Subject hint |
| --- | --- | --- | --- |
| Match rescheduled by host | Both teams' players | "Your match has been moved to Apr 24 at 19:30." | "Tournament match rescheduled" |
| Round complete, next round unlocked | Remaining players | "Round 1 complete. Semi-finals are scheduled." | "Tournament: semi-finals scheduled" |

### Edge Cases

| Event | Recipient | Email/body copy hint | Subject hint |
| --- | --- | --- | --- |
| Player removed from team by host | Removed player | "You were removed from Los Galacticos for Spring Cup." | "You were removed from a tournament team" |
| Captain leaves their own team | Other team members | "Diego left. Mariana is now captain." | "New team captain" |

See [`open-questions.md`](./open-questions.md) for roster-change semantics after
the bracket is locked.

## Implementation Notes

- Add `TournamentNotificationService` mirroring the shape of
  `MatchNotificationService`, but email-only for the first implementation.
- Each method should take the tournament plus relevant participants and delegate
  to `ThymeleafMailTemplateRenderer.renderTournament*Notification(...)`.
- Follow the per-recipient preferred-language pattern from
  `MatchNotificationServiceImpl`.
- Both Spanish and English templates must exist for every email.
- Deduplicate batch recipients so only one email per user is sent.
- Do not log email bodies, raw personal data, tokens, or secrets.
- Do not implement email throttling/coalescing unless the existing mail
  infrastructure already supports it cleanly.

## Templates To Add

First spine:

```text
renderTournamentBracketGeneratedNotification(...)
renderTournamentMatchResultNotification(...)
renderTournamentMatchWalkoverNotification(...)
renderTournamentCompletedNotification(...)
renderTournamentCancelledNotification(...)
renderSoloPoolAssignmentNotification(...)
```

Expanded team-draft slice:

```text
renderTournamentDraftInvitationNotification(...)
renderTournamentDraftInviteResponseNotification(...)
renderTournamentTeamLockedNotification(...)
renderTournamentRoundCompleteNotification(...)
renderTournamentMatchScheduledNotification(...)
```

Each `MailContent`-returning method must localize through the existing message
helper pattern and include the tournament detail URL in the CTA.
