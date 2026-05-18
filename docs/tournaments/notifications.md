# Tournaments — Notifications

The platform already has a robust notification pattern via `MatchNotificationService` and `ThymeleafMailTemplateRenderer`. **Tournaments must follow the same pattern**: every event-driven notification is dispatched simultaneously via in-app record and templated email. There is no email-link confirmation flow — accept/decline actions are taken in-app on either entry point.

## Dual-channel principle

For every notification type listed below:

1. The system writes an in-app record (visible in the notification bell + relevant view).
2. The system dispatches a templated email to the recipient.
3. Both contain the same information and the same call-to-action.
4. The CTA from the email opens the in-app view for the user to act.

This matches `MatchParticipationServiceImpl.dispatchMatchInvitation` precisely.

## Notification catalog

### Registration phase

| Event | Recipient | In-app copy hint | Email subject hint |
|---|---|---|---|
| Tournament published in feed | (none — passive discovery) | — | — |
| Player joined solo pool | Host | "Mariana joined Spring Cup as a solo player" | "New solo sign-up for Spring Cup" |
| Player left solo pool | Host | "Mariana left the solo pool for Spring Cup" | "Solo sign-up withdrew from Spring Cup" |
| Team-draft invitation sent | Invited user | "Diego invited you to Los Galácticos for Spring Cup" | "Diego invited you to Los Galácticos" |
| Team-draft invite accepted | Captain | "Mariana accepted your invite to Los Galácticos" | "Mariana joined your team" |
| Team-draft invite declined | Captain | "Mariana declined your invite to Los Galácticos" | "Mariana declined your team invite" |
| Team locked in (5/5 confirmed) | All team members | "Los Galácticos is in the bracket. Spring Cup starts Apr 23." | "Your team has secured a tournament slot" |
| Team-draft disbanded by captain | All invited / accepted members | "Diego disbanded Los Galácticos" | "Team draft cancelled" |
| Registration closes within 24h | Players in solo pool + drafts still open | "Spring Cup registration closes tomorrow" | "Last day to lock in your team" |

### Transition: registration → bracket

| Event | Recipient | In-app copy hint | Email subject hint |
|---|---|---|---|
| Solo player auto-assigned to team | Assigned player + the other 4 squad members | "You're on Solo squad #1 — your first match is Apr 23 · 18:00 vs The Reds" | "Your tournament team is set" |
| Bracket generated and scheduled | All players in the tournament | "Spring Cup bracket is live. Your first match: today · 18:00 vs FC Pampa." | "Your first tournament match is scheduled" |
| Tournament under-capacity → cancelled | All players | "Spring Cup was cancelled — not enough teams." | "Spring Cup has been cancelled" |

### In progress

| Event | Recipient | In-app copy hint | Email subject hint |
|---|---|---|---|
| Match rescheduled by host | Both teams' players | "Your match has been moved to Apr 24 · 19:30" | "Tournament match rescheduled" |
| Winner declared (you won) | Winning team's players | "You beat FC Pampa. Next: vs Norte United on Apr 24 · 18:00" | "You advanced to the semi-finals" |
| Winner declared (you lost) | Losing team's players | "FC Pampa took it. Spring Cup keeps going — follow the bracket." | "Your tournament run has ended" |
| Walkover recorded against you | Forfeiting team | "You were marked as a no-show for the Spring Cup quarter-final" | "Walkover recorded — tournament run ended" |
| Round complete, next round unlocked | All remaining players | "Round 1 complete. Semi-finals are scheduled for Apr 24." | "Tournament: semi-finals scheduled" |
| Tournament completed (you won the cup) | Champion team's players | "🏆 Los Galácticos won Spring Cup" | "You won the Spring Cup" |
| Tournament completed (you watched) | All players who participated and lost earlier | "Spring Cup is over — Los Galácticos took it. Leave a review for Diego." | "Spring Cup has concluded" |
| Tournament cancelled mid-play | All remaining players | "Spring Cup was cancelled by the host" | "Tournament cancelled" |

### Edge: kicks and removals

| Event | Recipient | In-app copy hint | Email subject hint |
|---|---|---|---|
| Player removed from team by host | The removed player | "You were removed from Los Galácticos for Spring Cup" | "You were removed from a tournament team" |
| Captain leaves their own team | All other team members (one of them is promoted) | "Diego left. Mariana is now captain of Los Galácticos." | "New team captain" |

See `open-questions.md` for what happens when a player is removed or a captain leaves *after* the bracket has been locked.

## Implementation notes

- Add `TournamentNotificationService` interface mirroring `MatchNotificationService`. Each method takes the tournament + relevant participants and delegates to `ThymeleafMailTemplateRenderer.renderTournament*Notification(...)` for email content + writes an in-app record.
- Follow the per-recipient preferred-language pattern from `MatchNotificationServiceImpl.testNotifyMatchUpdatedUsesRecipientPreferredLanguage`. Both Spanish and English templates must exist for every notification type.
- For batch notifications (e.g. "bracket generated → notify all 40 players"), reuse the deduplication pattern from `MatchNotificationServiceImpl.testNotifyRecurringMatchesUpdatedDeduplicatesAffectedParticipants` so that one row per recipient is created even if a player is on multiple drafts within the same tournament (shouldn't happen but defend anyway).
- The notification bell badge counts unread tournament notifications the same way it counts match invites today — no separate counter.
- Email throttling: do not send the "you advanced" + "next round scheduled" notifications as two separate emails to the same player — combine into a single email when both fire within a short window. (Same as how match-updated emails are combined today.)

## Templates to add to `ThymeleafMailTemplateRenderer`

```
renderTournamentDraftInvitationNotification(...)
renderTournamentDraftInviteResponseNotification(...)    // accepted | declined
renderTournamentTeamLockedNotification(...)
renderTournamentBracketGeneratedNotification(...)
renderTournamentMatchScheduledNotification(...)
renderTournamentMatchResultNotification(...)            // win | loss
renderTournamentMatchWalkoverNotification(...)
renderTournamentRoundCompleteNotification(...)
renderTournamentCompletedNotification(...)              // champion | participant
renderTournamentCancelledNotification(...)
renderSoloPoolAssignmentNotification(...)
```

Each `MailContent`-returning method must localise via the existing `message(key, locale)` helper and include the tournament-detail URL in the CTA.
