# Flow — Player During Play

The participant's experience once the bracket has been drawn and the tournament is `IN_PROGRESS`. These views are symmetric to the host views in `host.md` §3 but show only what the player can do, not declare-winner controls.

> Wireframe references in this doc: §7.

---

## Where players access these views

| Entry point | Lands on |
|---|---|
| Notification ("bracket generated" / "your first match scheduled") | Player live bracket view |
| Notification ("you advanced") | Advanced state view |
| Notification ("you lost") | Eliminated state view |
| "My events" → Tournaments tab | List of tournaments, drill in to player live bracket |
| Tournament detail URL (the same one used during registration) | Player live bracket view |

---

## 1. My Events — Tournaments tab

The existing "My Events" screen gains a new sub-tab "Tournaments" alongside the existing "Upcoming" and "Past" tabs.

### Layout

- Tab strip at the top: Upcoming (N) · Past (N) · **Tournaments (N)**
- Below the strip: a vertical list of tournament cards the user is currently or recently in.

### Tournament card (in My Events)

For each tournament:

- Banner image (same size as event card)
- Status badge top-left: "🏆 TOURNAMENT · LIVE" (or REGISTRATION / COMPLETED)
- Title
- Their team and current round: "Team: Los Galácticos · Quarter-finals"
- A `⚡ YOUR NEXT MATCH` callout panel (tinted), if applicable:
  - Bold line: "vs FC Pampa · today · 18:00"
  - Subtitle: "Club Norte · Pitch 2"
- Two buttons: "View bracket" (primary) / "Match details"

### Acceptance criteria

- The "your next match" callout only appears if the player still has a scheduled or awaiting match.
- Completed tournaments stay in this tab for at least 30 days after completion, then move to "Past".
- The card is interactive in its entirety — tapping anywhere outside the buttons opens the live bracket view.

---

## 2. Live bracket — player view

This is the symmetric counterpart to the host's live bracket. Uses the [bracket + match focus](../ui-patterns.md#bracket-view--bracket--match-focus) layout.

### Layout

- Header: tournament title, sub-line "Quarter-finals · 1 of 4 done", status pill "🟢 In progress"
- **Left column** — bracket grid. The player's team is **bold and highlighted** across every cell it appears in (current + projected). A small "← you" annotation marks their team's R1 cell.
- **Right column** — match-focus rail, defaulting to the player's next un-played match.

### Right-rail content for "your match"

- Eyebrow: "⚡ YOUR MATCH · R{N}"
- Both team cards (the player's team on top with a stronger border, opponent below)
- Date+time (bold), venue, countdown ("Starts in 2h 14m") or status ("Awaiting host's call")
- Primary CTA: depends on match state — see table in [ui-patterns.md → bracket view](../ui-patterns.md#bracket-view--bracket--match-focus)
- Secondary actions: "See teammates", "Add to calendar"
- Footer hint: "If you win: semi · Apr 24"

When the player clicks a different match in the bracket (e.g. to see their next round opponent), the rail switches to that match's read-only details.

### Acceptance criteria

- The player's team name renders bold and with a subtle emerald-tinted background everywhere it appears in the bracket (not just R1).
- The rail's default selection logic: earliest un-played match the player's team is in. If they have been eliminated, the earliest un-played match overall.
- "Get directions" opens the venue in the user's mapping app (deep-link).
- "Add to calendar" generates an .ics download with match title, location, and start/end times.

---

## 3. Awaiting host's call

A specific intermediate state: the match's `scheduled_at` has passed, but the host hasn't declared a winner yet. This view exists to manage the in-between gap so players don't think the system is broken.

### Layout

A focused single-card screen (not the bracket):

- Eyebrow: "QUARTER-FINAL · MATCH 1"
- Both teams stacked, "vs" between them
- Status pill: "⏳ awaiting host"
- Tinted info card: "Match has played. {Host name} needs to confirm who won. You'll get a notification (in-app + email) the moment they do."
- Secondary action: "Report a problem to host" (opens a simple message form to the host)

### Acceptance criteria

- This screen replaces the rail content for any match the player is in that has reached `AWAITING_RESULT` status.
- The "Report a problem" form sends a tournament-context message to the host's existing inbox (no new infrastructure).

---

## 4. After the match — advanced or eliminated

### 4a. Advanced

When the host declares the player's team as the winner of their match:

- The player receives the `match_result` notification (winner variant).
- Tapping the notification lands on a celebratory state screen:

Layout:

- Eyebrow: "✓ ADVANCED TO {next round name}"
- Headline: "You beat {opponent team name}."
- Subtitle: "One step closer to the cup."
- "Next match" panel showing:
  - Match identifier ("SEMI-FINAL · MATCH 1")
  - Both teams
  - Date / time / venue
- Two buttons: "View bracket" (primary) / "Share the news"

### 4b. Eliminated

When the host declares the player's team as the loser of their match:

- The player receives the `match_result` notification (loser variant).
- Tapping the notification lands on a softer state screen:

Layout:

- Eyebrow: "SEASON OVER FOR YOU"
- Headline: "{Opponent team name} took it."
- Subtitle: "Tough match. The Cup keeps going — follow it through."
- "Still to play" panel listing the remaining matches in the tournament
- Two buttons: "Follow the bracket" (primary) / "← Back to feed"
- Tertiary: "Leave a review for the host" (only enabled if tournament is `COMPLETED` and the user hasn't reviewed the host before)

### Acceptance criteria

- The advanced/eliminated states are reachable directly via the notification CTA — no requirement to navigate via My Events.
- Eliminated players keep access to the live bracket view (they can still "follow the bracket"). Their team continues to appear in the bracket grid with their loss reflected.
- The review CTA only appears after the entire tournament has completed (not after the player's individual elimination).

---

## 5. Champion view

When the player's team wins the final match:

- All team members receive the `tournament_completed` notification (champion variant).
- The tournament's `status` is now `COMPLETED`.
- The bracket view's top banner is replaced (for these users only) with the dedicated champion view.

Layout:

- Large 🏆 emoji or icon centered
- Eyebrow: "CHAMPION"
- Headline: "{Team name}" (large, bold)
- Subtitle: "{Tournament name} · {completion date}"
- Squad list (all 5 team members)
- Three buttons: "Share results" (primary) / "See full bracket" / "Leave a review for the host"

### Acceptance criteria

- The champion view appears only for users on the winning team. Other players (eliminated earlier) see the standard "tournament completed" state with the champion banner referencing the winning team name.
- "Share results" deep-links to the public tournament URL. No image generation in v1.
- The squad list shows captain marker (★) on the original team draft captain. For SOLO_POOL teams, the captain marker is absent (synthetic team has no captain).

---

## State transitions a player sees

```
[Bracket generated]
       │
       ├──► Live bracket (your match scheduled)
       │            │
       │            ├──► [Match plays, host declares]
       │            │            │
       │            │            ├──► Advanced ───► next "your match" scheduled
       │            │            │
       │            │            └──► Eliminated ───► follow-only mode
       │            │
       │            └──► [Match time passes, no declaration]
       │                         │
       │                         └──► Awaiting host's call
       │
       └──► [Tournament cancelled]
                    │
                    └──► "Tournament cancelled" view + return to feed
```

After `COMPLETED`:

- Winners → champion view
- Losers → standard "tournament concluded" view with winner credited
- Both → can leave a review (if eligible per PRD §14)

---

## Notifications relevant to this flow

All dual-channel. See `notifications.md` for full copy.

- `bracket_generated` → "Your first match is scheduled"
- `match_scheduled` (when later-round match's schedule is finalised)
- `match_rescheduled` (when host edits a future match)
- `match_result` win variant → opens to §4a
- `match_result` loss variant → opens to §4b
- `match_walkover` → opens to §4b with walkover-specific copy
- `round_complete` → "Round 1 complete. Semi-finals are scheduled for Apr 24."
- `tournament_completed` champion variant → opens to §5
- `tournament_completed` participant variant → opens to §4b "tournament concluded"
- `tournament_cancelled` → opens to standalone cancelled screen
