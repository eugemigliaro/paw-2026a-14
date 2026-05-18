# Tournaments — Open Questions and Edge Cases

This document lists decisions that are intentionally **not** resolved in v1 wireframes/specs. Each item has enough context for the implementer to either escalate or pick a sensible default — but make the choice explicit, do not silently invent one.

Items are tagged:

- **[BLOCKING]** — must be decided before that flow can be built.
- **[DEFERRED]** — has a v1 default, but flagged for product follow-up.
- **[POST-V1]** — explicitly out of scope; listed so it isn't accidentally built.

---

## Registration & team formation

### Under-capacity registration close [BLOCKING]

When `registration_closes_at` is reached and fewer than `bracket_size` teams have locked in, what happens?

Possible answers:

- A: Auto-cancel the tournament + notify everyone. (Cleanest, safest.)
- B: Auto-extend the window by 48h once, then cancel. (Friendlier.)
- C: Allow the host to bye-fill missing slots (i.e. accept smaller brackets — 6 teams in an 8-slot bracket with 2 byes). (Most flexible, most complex.)

The wireframes assume A. Confirm before building the auto-transition job.

### Captain leaves their own team mid-registration [BLOCKING]

If a captain abandons their own draft (clicks "Disband draft" or removes themselves):

- Default: draft is disbanded, every accepted invitee is notified, slots are released back into the pool.
- Question: should we instead promote the longest-tenured accepted invitee to captain?

The wireframes show "Disband draft" as a deliberate action. We have not designed a "leave team and pass captaincy" flow.

### A user is in a draft + accepts another draft's invite [BLOCKING]

Service-layer rule: a user can be on at most one team or one solo entry per tournament. What does the second action do?

- A: Block the second action with an error ("You're already on a team in this tournament").
- B: Implicit-leave the first team and join the second.

Default A. (B is too easy to do accidentally.)

### Solo-pool grouping logic [DEFERRED]

When registration closes, how does the auto-bundler group solo players into teams?

v1 default:

- Take all solo entries.
- Group them into teams of exactly `team_size`.
- If the number is not divisible by `team_size`, leftover solos are silently dropped from the tournament with a notification ("Sorry, we couldn't fit you on a team this time").
- Synthetic team names: "Solo squad #1", "Solo squad #2", etc.

Post-v1: let the host preview and edit the synthetic teams before locking the bracket.

### Solo player wants to switch out of pool into a draft after joining solo [DEFERRED]

The wireframes have a "Leave the solo pool" button. We did not design an "accept a draft invite while already in the pool" flow specifically.

Default: declining the pool happens implicitly when the user accepts a draft invite. The notification subtly mentions both ("You joined Los Galácticos and were removed from the solo pool").

---

## Bracket and matches

### Per-match cancellation [BLOCKING]

The host UI has a "Cancel match…" button. We did not fully design what cancellation means:

- A: Cancel = cancel the entire tournament. (Simple, brutal.)
- B: Cancel = mark this match as void → host must declare a winner manually anyway (treat as walkover for one or both teams).
- C: Cancel = pause the whole bracket until host re-schedules and removes the cancel flag.

Recommend B — the host either picks one side to advance (walkover), or marks neither and the whole tournament becomes stuck (which is a strong nudge to use walkover instead).

### Host wants to undo a declared winner [BLOCKING]

Currently the wireframes have no "undo" button on a declared match. A host who mis-clicks needs a remedy.

Options:

- A: A 30-second undo toast immediately after declaration. After that, no changes.
- B: Allow undo until the *next* round's match begins.
- C: Admin/mod-only override path.

Recommend A + C combined (player-facing safety net is the 30s toast; admin can fix later if a real dispute arises).

### What does "walkover" mean to the losing side? [DEFERRED]

In the wireframes, "Record walkover / forfeit" is a host action. Defaults:

- The losing team's players get a notification: "Walkover recorded — your tournament run ended".
- Their participation history shows the match with status `WALKOVER` and the team they forfeited to.
- Reputational consequence: none in v1. (Possible future: walkovers count toward a no-show flag on the player profile.)

### Mid-tournament team-member changes [BLOCKING]

Can a team's roster change after the bracket has locked? (Player gets injured, etc.)

Default: **no**. Locked-in rosters are final from the moment the bracket is generated. This avoids ringers and last-minute swaps.

Alternative: allow the captain to swap one player per round with host approval. (More complex; explicitly defer.)

### Empty bracket positions ("BYE") [POST-V1]

Single-elim with non-power-of-two team counts requires BYE slots. v1 hard-codes `bracket_size ∈ {4, 8, 16}` to avoid this. If the under-capacity question above is answered with "C" (bye-fill), this becomes a real requirement.

---

## Notifications and copy

### Per-recipient notification frequency [DEFERRED]

A player on a 16-team tournament could receive 6+ notifications in a single evening (their match advances, opponents change, schedule edits). Do we want a daily digest option?

v1: no digest, every event triggers immediate dual-channel delivery. (Same as match invites today.) Revisit if user feedback shows fatigue.

### Notification preferences per channel [POST-V1]

The platform currently has no "mute email but keep in-app" toggle. Adding it is bigger than tournaments. Out of scope.

---

## Discovery and feed

### Should tournaments be filterable separately in the feed? [DEFERRED]

The wireframes show a `🏆 Tournaments` filter chip alongside category chips, but it is just a visual placeholder. Decision: yes for v1, implement it as a single-toggle filter (`?type=tournament`).

### Where do completed tournaments live? [DEFERRED]

Currently completed tournaments fall out of the active feed. For players in them, they live in "My Events → Past". For non-participants, no easy way to find them again.

Default: no public "browse past tournaments" listing in v1. Direct URLs continue to work.

---

## Reviews

### Review trigger after tournament completion [BLOCKING]

PRD §14 says "only users who attended an event can leave a review to a host". Tournaments are events; participants attended; the rule applies. Question:

- Does each player get one review per tournament? Or one review per host (as the existing rule says, "one review per host")?

The PRD says "one review per host". So a player who has already reviewed Diego from a prior event cannot review him again after this tournament. That is the v1 behaviour. Surface a polite message ("You've already reviewed this host") in the post-tournament screen for repeat reviewers.

### Player-to-player reviews [POST-V1]

Some platforms let teammates rate each other. Out of scope.

---

## Performance and scale

### Polling vs. push for live bracket updates [POST-V1]

A player watching the bracket sees stale data until they refresh. For v1 this is acceptable; a "Refresh" button on the bracket view is sufficient. Real-time updates (polling or websockets) is post-v1.

### Notification fan-out for large tournaments [DEFERRED]

A 16-team tournament with team_size=5 = 80 players. "Bracket generated" sends 80 notifications. Use the existing batching from `MatchNotificationServiceImpl` to keep this efficient.

---

## Future scope (intentionally not designed)

These are listed so they aren't accidentally added to v1 scope:

- Double elimination, round-robin, group stage
- Bracket sizes other than 4/8/16
- Skill-based or rating-based auto-seeding
- Public-team join ("see existing teams looking for players")
- Cross-tournament leaderboards / player rankings
- Spectator follow / push notifications for non-participants
- In-app chat per team or per match
- Live scoring and per-match statistics
- Shareable bracket image / OG card generation
- Payment processing for tournament entry fees
- Multi-venue tournaments with venue-routing per match
- Tournament series (e.g. a season of weekly tournaments tied together)
