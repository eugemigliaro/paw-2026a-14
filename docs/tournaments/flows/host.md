# Flow — Host

The host's complete journey: creating the tournament, seeding the bracket once registration closes, and running it match-by-match.

> Wireframe references in this doc: §1 (create wizard), §5 (seed bracket), §6 (run).

---

## 1. Host creates a tournament (wizard)

### Entry point

In Host mode, the existing "Create event" page gets a new option at the top: "Create tournament". Clicking it enters the wizard. The wizard reuses the existing create-event chrome and styling — sectioned form on the left, progress + summary rail on the right.

### Steps

**Step 1 — Basics**

Fields:

- Title (required)
- Sport / category (required, single-select from existing categories)
- Skill level (required: Beginner / Intermediate / Advanced)
- Short description (optional)
- Banner image (optional, same upload as event banner)

**Step 2 — Format & team rules**

Fields:

- Bracket format: only **Single elimination** is selectable. Render the other formats (Double elim, Round-robin) as disabled chips with a "later" badge so the future intent is visible.
- Number of teams: 4 / 8 / 16 (segmented control, default 8)
- Team size (players per team): number input, 1–11 typical, no hard upper bound, default 5
- Two checkboxes (both default ON, must have at least one checked):
  - Allow solo sign-ups (host assigns to a team)
  - Allow team drafts (captain invites friends)

Right rail shows derived numbers in real time: `bracket_size × team_size = total players` and `(bracket_size - 1) total matches`.

**Step 3 — Schedule**

Fields:

- Registration opens at (datetime, default: now)
- Registration closes at (datetime, must be > opens_at + 24h)
- Per-round defaults (one row per round = `log2(bracket_size)` rows):
  - Round name (auto-generated: "Quarter-finals", "Semi-finals", "Final")
  - Default date+time
  - Default venue

The per-round defaults are just defaults — the host can override per-match later.

**Step 4 — Review & create**

Read-only summary of all fields with a single "Create tournament" button. Clicking it:

1. Persists the tournament as `status = REGISTRATION`.
2. The tournament becomes visible in the feed and at its public URL.
3. A schedule job is registered to flip status to `BRACKET_SETUP` at `registration_closes_at`.

Also visible on this step: "Back to schedule" (return to step 3).

### Acceptance criteria

- Wizard validates each step before allowing Continue.
- Refresh during the wizard can restore the user to the same step with same field values by holding state in session.
- Creating a tournament with no banner is allowed (placeholder rendered in the feed card).
- All wizard fields except description and banner are required.

### Edge cases

- After creation, do **not** allow changing `bracket_size`, `team_size`, or `format`. Show those fields as disabled with a tooltip ("Set at creation").

---

## 2. Host seeds the bracket

### Entry point

Two ways to land here:

1. The host clicks the "🎲 Generate bracket" CTA on the tournament page once it has transitioned to `BRACKET_SETUP`.
2. The auto-transition job fires a notification ("Spring Cup registration closed — generate your bracket"); the in-app/email CTA brings them here.

### Steps

**Step A — Registration closed view**

Read-only summary of who registered: a 2-column grid of locked teams (with origin labels: TEAM_DRAFT or SOLO_POOL). Single primary CTA: "🎲 Generate bracket".

**Step B — Bracket draft view**

The bracket is now visible using the [bracket + match focus](../ui-patterns.md) layout. Auto-seeded with a random shuffle of locked teams.

Host actions on this view:

- **Drag-swap**: drag any team card onto another team card → both swap positions. Visual feedback (highlight the source and target).
- **Select**: click a team / match card → the right rail shows team details and "Swap with…" / "Mark as #1 seed" buttons.
- **🎲 Re-shuffle**: re-randomises all seeds. Cannot be undone except by Undo.
- **↶ Undo**: undoes the last swap or shuffle. Maintains a stack of at least the last 10 actions.

**Step C — Schedule round 1**

After confirming the bracket, the host lands on a per-round-1-match scheduler. Each match row has:

- Match identifier ("M1", "M2", "M3", "M4")
- Teams ("Los Galácticos vs FC Pampa")
- Editable date+time field (pre-filled with the round default from the wizard)
- Editable venue field (pre-filled with the round default)
- "edit" button to open the match in full edit (date, time, venue, notes)

Single CTA at the bottom: "Publish round 1 schedule". Clicking it:

1. Persists the round-1 schedules.
2. Fires `bracket_generated` notifications to all 80 players.
3. Transitions the tournament status to `IN_PROGRESS`.

### Acceptance criteria

- Auto-seed must produce a valid bracket every time (no team appears in two cells, etc.).
- Drag-swap works on touch devices (long-press to pick up, drop on target). On desktop, plain drag is sufficient.
- Re-shuffle and Undo are disabled (greyed out) once status transitions to IN_PROGRESS.
- Round-1 schedule defaults cannot leave any match without a date+time before allowing Publish.

### Edge cases

- If a previously-locked team gets disbanded between registration close and bracket generation (e.g. a captain abuses the system), do **not** allow generation — surface an error: "Team X is no longer valid; please review participants".
- Solo-pool grouping happens during the auto-seed step. See [open-questions.md → solo-pool grouping logic](../open-questions.md#solo-pool-grouping-logic-deferred).

---

## 3. Host runs the tournament

### Entry point

The host's home for a live tournament is the [bracket + match focus](../ui-patterns.md) view. The right rail is the action surface.

### Per-match flow: declare winner

1. Host taps a match in the bracket.
2. The right rail shows the match with two team cards side-by-side, plus a primary "Declare winner" CTA.
3. Tapping "Declare winner" opens a modal:
   - "ROUND N · MATCH M" eyebrow, "Who won?" headline
   - Two large team cards side-by-side; tapping one selects it (visual: highlighted card with "✓ Winner")
   - Secondary action: "↳ record walkover / forfeit" (opens the walkover sub-flow, see below)
   - Primary action: "Confirm winner → advance"
4. On confirm:
   - Persist `winner_team_id` on the match.
   - Propagate winner into the child match's appropriate slot.
   - Update the child match's status from PENDING to SCHEDULED (using the round defaults if no per-match override exists).
   - Fire `match_result` email notifications to all players on both teams (winner and loser variants).
   - Fire `round_complete` notification if this was the last un-played match in the round.
   - If this was the final match, transition tournament to `COMPLETED` and fire `tournament_completed` notifications.

### Walkover / forfeit sub-flow

When the host clicks "↳ record walkover / forfeit" from the declare-winner modal:

1. Modal pivots to "Which team is forfeiting?" with the same two team cards.
2. Host taps a team → that team forfeits; the other team advances.
3. Match status is set to `WALKOVER`, not `DONE`.
4. The forfeiting team's players get a special "walkover recorded against you" notification.

### Per-match flow: edit schedule

1. Host taps a match → right rail shows "Edit schedule" button.
2. Tapping it opens an inline editor (or modal on mobile) with date, time, venue, notes fields pre-filled.
3. On save:
   - Persist the new schedule.
   - Fire `match_rescheduled` notifications to all players on both teams.

Only future matches (status SCHEDULED or PENDING) are editable. Done and walkover matches are read-only.

### Per-round flow: unlock the next round

After all matches in round N are DONE or WALKOVER:

1. The page shows a banner: "Round N complete ✓ — ready for {next round name}"
2. Single CTA: "Unlock & notify · {next round name}"
3. The next round's matches are already populated with teams (via parent-match propagation) and pre-filled schedules (from the wizard defaults). The right rail lets the host edit each before unlocking.
4. Clicking the CTA fires `round_complete` notifications to all remaining players.

### Acceptance criteria

- The bracket reflects winner declarations in real time without requiring a full page refresh (use Turbo, HTMX, or simple re-render — your choice).
- Walkovers count as a winner declaration for bracket-advance purposes.
- The host cannot declare a winner for a match whose `team_a_id` or `team_b_id` is still null (parent matches not yet decided).
- The host has a 30-second undo toast after declaring a winner — see [open-questions.md → undo declared winner](../open-questions.md#host-wants-to-undo-a-declared-winner-blocking).

### Edge cases

- "Cancel match…" button — semantics unresolved, see [open-questions.md → per-match cancellation](../open-questions.md#per-match-cancellation-blocking).
- "Notify players" button — sends a custom manual email to all participants. Implementation: a simple textarea modal + email dispatch. No formatting beyond plain text in v1.

### Completed state

When the final match is declared:

- Tournament status flips to `COMPLETED`.
- All players see the 🏆 CHAMPION banner over the bracket.
- The winning team's players additionally see the dedicated champion view (see `player-during-play.md` §5).
- Review CTAs become available to all participants (one per host per user, per PRD §14).
