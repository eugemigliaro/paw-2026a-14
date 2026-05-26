# Tournament Lifecycle

> Implementation note: read [`feature-brief.md`](./feature-brief.md) first.
> New code should use `BRACKET_SETUP`, not the older `LOCKED_BRACKET` wording.

Every tournament moves through four active phases plus terminal outcomes. The
phase determines what is visible to whom and which actions are allowed.

## State Machine

```
+--------------+  close reg   +---------------+  unlock R1   +-------------+  final winner  +-----------+
| REGISTRATION | -----------> | BRACKET_SETUP | -----------> | IN_PROGRESS | ------------> | COMPLETED |
+--------------+              +---------------+              +-------------+               +-----------+
       ^
       |
       +-- created by host from a valid tournament form
```

Transitions are one-way. There is no save-and-resume draft state and no reopen
registration path in v1.

## REGISTRATION

The tournament is live in the feed. Players can join solo or start team drafts.
Bracket matches have not been generated.

Allowed actions for the host:

- Edit non-structural fields, such as description, banner, and venue defaults.
- Cancel the tournament, with notification to signed-up players.
- View the live participant list and team drafts.
- Cannot edit `team_size`, `bracket_size`, or `format` after creation.

Allowed actions for players:

- Join the solo pool.
- Leave the solo pool.
- Start a team draft, invite others, swap declined invitees, or disband.
- Accept or decline an invitation to another user's draft.
- A user can be in at most one team or in the solo pool per tournament.

Auto-triggers:

- When the registration window's `closes_at` is reached, the tournament
  transitions to `BRACKET_SETUP`.

## BRACKET_SETUP

Registration is closed. The bracket exists or is being shaped, but matches have
not started.

Allowed actions for the host:

- Generate the initial bracket.
- Drag-swap any two teams in the bracket.
- Re-shuffle or undo.
- Set or override date, time, and venue per round-one match.
- Confirm the bracket and transition to `IN_PROGRESS`.

Disallowed for the host:

- Re-opening registration.
- Adding or removing teams individually. The only team-removal path is to
  cancel the entire tournament.

Player-facing during this phase:

- Tournament status shows bracket setup in the feed.
- Players see their team and squad, but not their opponent until the bracket is
  published.
- Players cannot leave their team during this phase.

## IN_PROGRESS

Matches are being played. The host advances the bracket one match at a time.

Allowed actions for the host:

- Edit date, time, and venue for any future match.
- Declare a winner for any awaiting match.
- Cancel the tournament.
- Send a manual notification to all participants.

Disallowed for the host:

- Re-seed the bracket.
- Change the winner of a previous match. Manual override is a future ask.

Player-facing during this phase:

- Live bracket with player's team highlighted.
- "Your next match" rail with date, venue, opponent, and countdown.
- After a loss, an eliminated view that still allows following the bracket.
- After a round ends, an advanced view with next match details.

Sub-states are informational, not separate enum values:

- *Active match available* - at least one match in the current round is past its
  scheduled time but has no winner yet.
- *Between rounds* - current round is complete and the next round has not
  started.
- *Final pending* - only the final match remains.

## COMPLETED

The final match has a declared winner. The tournament is read-only.

Allowed actions:

- Anyone can view the final bracket.
- Winning team gets the champion view.
- Players can leave a review for the host, following the existing review rules.

Disallowed:

- No regular edits of any kind. Admin/mod users retain moderation powers.

## CANCELLED

The tournament was cancelled before completion. It remains readable where the
product chooses to show cancelled events, but no participation or bracket
actions are available.

## Allowed Actions Matrix

| Action | REGISTRATION | BRACKET_SETUP | IN_PROGRESS | COMPLETED |
|---|:---:|:---:|:---:|:---:|
| Host: edit format / team size | no | no | no | no |
| Host: edit description / banner | yes | yes | yes | no |
| Host: cancel tournament | yes | yes | yes | no |
| Host: generate bracket | no | yes | no | no |
| Host: re-seed / drag-swap | no | yes | no | no |
| Host: edit match schedule | no | yes (R1) | yes (future only) | no |
| Host: declare winner | no | no | yes | no |
| Player: join solo / leave pool | yes | no | no | no |
| Player: start / disband team draft | yes | no | no | no |
| Player: accept / decline invite | yes | no | no | no |
| Player: leave team | yes | no | no | no |
| Player: view bracket | partial | yes | yes | yes |
| Player: leave review | no | no | no | yes |

"Partial" in `REGISTRATION` means players see a teams-forming list, not a
bracket.

## Trigger Summary

| Trigger | From | To | Source |
|---|---|---|---|
| Create tournament | none | REGISTRATION | Manual host submit |
| Reg window closes | REGISTRATION | BRACKET_SETUP | Auto scheduled task or host action |
| Confirm bracket | BRACKET_SETUP | IN_PROGRESS | Manual host action |
| Final declared | IN_PROGRESS | COMPLETED | Auto when winner of final match is declared |
| Cancel | REGISTRATION/BRACKET_SETUP/IN_PROGRESS | CANCELLED | Manual host or admin action |

See `open-questions.md` for under-capacity registration close behavior and
per-match cancellation semantics.
