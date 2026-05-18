# Tournaments — UI Patterns

This document captures the UI variants picked during wireframe planning and maps them to the existing design system. Follow these patterns; do not invent alternatives unless `open-questions.md` flags them for re-decision.

## Picked variants

### Bracket view — "bracket + match focus"

(Wireframe section §8, variant B)

Two-column layout:

- **Left column** — the bracket itself. Rounds left-to-right, matches stacked vertically per round. Each match is a small card with the two team names and a one-line status (date+time, "✓", or "awaiting result").
- **Right column** — a context rail. Width ~220px on desktop, collapses to a sticky bottom sheet on mobile. Shows the currently selected match in expanded form: both teams, full schedule details, status, and the relevant primary action.

Selection rules:

- Tap on any bracket match → that match becomes the rail's focus.
- Default selection when the screen first loads:
  - For the **host** in IN_PROGRESS state: the earliest match without a declared winner.
  - For a **player**: the player's own next un-played match. If they have been eliminated, the next un-played match in the round their loss happened.

The rail's primary action differs by audience and match state:

| Audience | Match state | Primary action |
|---|---|---|
| Host | AWAITING_RESULT | "Declare winner" |
| Host | SCHEDULED, future | "Edit schedule" |
| Host | DONE | (none — read-only) |
| Player (on this match) | SCHEDULED | "Get directions" |
| Player (on this match) | AWAITING_RESULT | (status text only — "Diego will confirm the result soon") |
| Player (not on this match) | any | (read-only details) |

### Tournament card in feed — "same as event card"

(Wireframe section §9, variant A)

A tournament card has **the same shape and chrome as a regular event card**. The only differentiation is a small badge in the top-left of the banner image:

- Background: `--color-surface` (white/paper).
- Border: standard 1.5px subtle neutral, **not** the 2px ink border used elsewhere.
- Top-left badge: `🏆 TOURNAMENT` — 10–11px, bold, dark text on white surface, 1.5px ink border, ~4px radius.
- Body: title, sport, date range (e.g. "Apr 23–26"), one metadata line (e.g. "3/8 teams · 4d to join").

This keeps the feed visually calm and trustworthy. The hero/takeover variant (§9C) is **reserved for the live/last-day featured slot only** and is not part of v1.

## Design-system mapping

These map straight to tokens from `docs/design.md`. Use these, do not invent new ones.

| UI element | Token / value |
|---|---|
| Primary CTA buttons ("Join solo", "Publish tournament", "Declare winner", "Get directions") | `color-primary` emerald background, white text, 12-16px radius |
| Secondary buttons ("Cancel", "Edit schedule") | White surface, 1.5px neutral border, dark text |
| Destructive buttons ("Disband draft", "Leave the solo pool", "Cancel tournament") | Red text + subtle border, never visually dominant |
| Status pills (in-feed "3/8 teams · 4d left", "LIVE", "AWAITING RESULT") | Standard chip styling, mint background for positive states, neutral for informational |
| "🏆 TOURNAMENT" badge | Compact label-style, dark text on paper surface, 1.5px ink border |
| Player's-team highlight in bracket | Bold weight on team name + emerald accent border on the match card |
| "Your next match" rail header | Tinted mint (`--color-surface-positive`) background with primary text |
| Champion banner | Tinted mint surface, larger weight ("you won"), single primary CTA |
| Bracket connector lines | 1.5px neutral, never harsh black |
| Section background (Create-tournament wizard right rail) | `--color-surface-muted` |

## Reusable UI primitives to build

These are net-new components. Build them once, use everywhere a tournament is shown:

1. `<TournamentCard>` — feed card variant of a tournament. Used in the event feed, in "My Events → Tournaments" tab, and in search results.
2. `<TournamentBadge>` — the small "🏆 TOURNAMENT" / "🏆 LIVE" / "🏆 CHAMPION" label. Variants by status.
3. `<BracketGrid>` — the bracket itself. Accepts a tournament + selectedMatchId, renders the columns and connectors. Click-through fires a `select(matchId)` callback.
4. `<BracketMatchCard>` — a single match cell within the bracket. Two teams + status line. Selectable state.
5. `<MatchFocusRail>` — the right-column context rail. Audience-aware (host/player), match-state-aware.
6. `<NextMatchCard>` — the "your next match" callout used in My Events. Same data shape as `<MatchFocusRail>` but standalone.
7. `<TeamRosterList>` — list of player names with captain marker. Used inside the draft screen, the team-detail view, and the focus rail.
8. `<TournamentLifecycleBadge>` — small inline status indicator ("Registration open", "In progress", "Completed"). Maps the tournament status enum to localised copy.

## Things to deliberately avoid

- **No bracket-as-canvas zoom/pan in v1.** The bracket has at most 16 teams (4 columns of matches); it fits on a desktop screen with the rail and stacks vertically on mobile without needing zoom.
- **No live "spectator" auto-refresh animations.** Page load + manual refresh is fine for v1. Adding polling/websockets is post-v1.
- **No bracket reveal animations.** When the bracket is generated, players see it at the next page load — no big reveal moment in v1.
- **No bracket sharing image generation.** "Share results" can deep-link to the tournament page; do not build a shareable image generator in v1.
- **No mobile-only round-stepper UI.** The bracket+match-focus layout was picked specifically to work on both desktop and mobile (rail becomes a bottom sheet on small screens). Do not also build the round-stepper as an alternate mobile UI.

## Mobile adaptation

For the bracket+rail layout below ~700px viewport width:

- The bracket scrolls horizontally with momentum, snapping to round columns.
- The match-focus rail becomes a **sticky bottom sheet** with a 80px peek showing the focused match summary. Tap to expand to full-height sheet with all rail actions.
- The "your next match" callout remains pinned at the top of the screen (above the bracket) for players in IN_PROGRESS state so they don't have to find it.

## Visual hierarchy rules

1. Whatever has the player's **next required action** is the loudest thing on the screen. (E.g. "Get directions" if their match is in 2 hours; "Declare winner" for the host on an awaiting match.)
2. The bracket itself is **visual context**, not the primary action surface. Keep it calm — most cells should be near-neutral; only the player's team and the selected match get emphasis.
3. Status pills and metadata should never compete with team names or CTAs.
4. Imagery (banner) is used for emotional anchor on the detail page and feed card. Do not put imagery inside the bracket or focus rail.
