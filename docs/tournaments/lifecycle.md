# Tournament Lifecycle

> Implementation note: read [`feature-brief.md`](./feature-brief.md) first.
> This lifecycle document is the original wireframe-era model. New code should
> use the implementation statuses defined in the brief, especially
> `BRACKET_SETUP` instead of `LOCKED_BRACKET`.

Every tournament moves through five phases. The phase determines what is visible to whom and which actions are allowed. **All other documents in this folder assume the lifecycle below; treat it as the foundation.**

## State machine

```
+--------+   publish    +--------------+  close reg   +-----------------+  unlock R1   +---------------+   final winner   +-----------+
| DRAFT  | -----------> | REGISTRATION | -----------> | LOCKED·BRACKET  | -----------> | IN PROGRESS   | --------------> | COMPLETED |
+--------+              +--------------+              +-----------------+              +---------------+                 +-----------+
                              ^                                ^
                              |                                |
                              +-- (auto) on reg-window opens   +-- (auto / host) when reg closes
```

Transitions are one-way. There is no "reopen registration" or "rewind to draft" path in v1.

## DRAFT

The Host is still in the wizard. The tournament does **not** appear in the feed. Nobody but the host can see it.

Allowed actions:

- Edit any field in the wizard
- Save and resume later
- Delete the draft
- Publish (only when all required wizard fields are valid)

Disallowed:

- Players cannot discover or join it (it has no public URL yet)

## REGISTRATION

The tournament is live in the feed. Players can join solo or start team drafts. Bracket has not been generated.

Allowed actions for the host:

- Edit non-structural fields (description, banner, venue defaults)
- Cancel the tournament (with notification to all signed-up players)
- View the live participant list and team drafts
- **Cannot** edit `team_size`, `bracket_size`, or `format` after publishing

Allowed actions for players:

- Join the solo pool (instant, no approval)
- Leave the solo pool
- Start a team draft, invite others, swap declined invitees, disband
- Accept / decline an invitation to someone else's draft
- A user can be in at most one team or in the solo pool per tournament

Auto-triggers:

- When the registration window's `closes_at` is reached, the tournament transitions to **LOCKED · BRACKET** automatically.

## LOCKED · BRACKET

Registration closed. The bracket exists but matches have not started. The host shapes the bracket here.

Allowed actions for the host:

- Generate the initial bracket (auto-seed: random ordering of locked teams + auto-built teams from the solo pool)
- Drag-swap any two teams in the bracket
- Re-shuffle / undo
- Set or override date / time / venue per round-1 match
- Confirm the bracket → fires "unlock R1" notification + transitions to **IN PROGRESS**

Disallowed for the host:

- Re-opening registration
- Adding or removing teams individually (the only "delete a team" path is to cancel the entire tournament)

Player-facing during this phase:

- Tournament status shows "Bracket forming" in the feed
- Players see their team and squad, but **not** their opponent yet (no leaking of in-progress seeding decisions)
- Players cannot leave their team during this phase (commitment is locked)

## IN PROGRESS

Matches are being played. The host advances the bracket one match at a time.

Allowed actions for the host:

- Edit date/time/venue for any **future** match
- Declare a winner for any **awaiting** match (no scores, just a winner)
- Record a walkover / forfeit for a match (counts as a winner declaration)
- Cancel a match (special — see open-questions.md)
- Send a manual notification to all participants

Disallowed for the host:

- **Cannot** re-seed the bracket (seeds are locked once round 1 starts)
- **Cannot** change the winner of a previous match (manual override is a future ask — see open-questions.md)

Player-facing during this phase:

- Live bracket with player's team highlighted
- "Your next match" rail with date / venue / opponent / countdown
- After a loss: "eliminated" view; can still follow the bracket
- After a round-end: "advanced" view with next match details

Sub-states (informational, not enforced as separate enum values):

- *Active match available* — at least one match in the current round is past its scheduled time but has no winner yet
- *Between rounds* — current round complete, next round not yet unlocked
- *Final pending* — only the final match remains

## COMPLETED

The final match has a declared winner. The tournament is read-only forever.

Allowed actions:

- Anyone can view the final bracket
- Winning team gets the 🏆 CHAMPION view
- Players can leave a review for the host (one per user per host, per existing PRD §14)

Disallowed:

- No edits of any kind. (Admin/mod elevated role retains the ability to delete the entire tournament if it must be moderated — same as any event today.)

## Allowed actions matrix

| Action | DRAFT | REG | LOCKED | IN PROGRESS | COMPLETED |
|---|:---:|:---:|:---:|:---:|:---:|
| Host: edit format / team size | ✓ | ✗ | ✗ | ✗ | ✗ |
| Host: edit description / banner | ✓ | ✓ | ✓ | ✓ | ✗ |
| Host: cancel tournament | ✓ | ✓ | ✓ | ✓ | ✗ |
| Host: generate bracket | ✗ | ✗ | ✓ | ✗ | ✗ |
| Host: re-seed / drag-swap | ✗ | ✗ | ✓ | ✗ | ✗ |
| Host: edit match schedule | ✗ | ✗ | ✓ (R1) | ✓ (future only) | ✗ |
| Host: declare winner / walkover | ✗ | ✗ | ✗ | ✓ | ✗ |
| Player: join solo / leave pool | ✗ | ✓ | ✗ | ✗ | ✗ |
| Player: start / disband team draft | ✗ | ✓ | ✗ | ✗ | ✗ |
| Player: accept / decline invite | ✗ | ✓ | ✗ | ✗ | ✗ |
| Player: leave team | ✗ | ✓ | ✗ | ✗ | ✗ |
| Player: view bracket | ✗ | partial | ✓ | ✓ | ✓ |
| Player: leave review | ✗ | ✗ | ✗ | ✗ | ✓ |

"Partial" in REG = sees teams forming list, not the bracket (it doesn't exist yet).

## Trigger summary

| Trigger | From | To | Source |
|---|---|---|---|
| Publish | DRAFT | REGISTRATION | Manual (host clicks "Publish") |
| Reg window closes | REGISTRATION | LOCKED · BRACKET | Auto (scheduled task at `registration_closes_at`) |
| Confirm bracket | LOCKED · BRACKET | IN PROGRESS | Manual (host clicks "Unlock & notify · semis/Round 1") |
| Final declared | IN PROGRESS | COMPLETED | Auto (when winner of final match is declared) |
| Cancel | any of REG/LOCKED/IN PROGRESS | (deleted / CANCELLED — TBD) | Manual (host or admin) |

See `open-questions.md` for: what to do when registration closes under-capacity, whether cancellation is a state or a deletion, and per-match cancellation semantics.
