# Tournaments — Overview

## What this feature is

A **Host** can publish a tournament — a structured competition between teams of players, organised as a single-elimination bracket. **Users** sign up either solo (the host assigns them to a team) or as part of a team they drafted with friends. The host runs the tournament during play by declaring a winner per match; results automatically advance teams up the bracket.

The feature extends the existing matches platform; it does not replace it. A tournament reuses many of the same primitives a regular match has (sport, venue, date, host, capacity) but adds bracket and team-roster structure on top.

## Why we're building it

The existing platform supports single matches and recurring matches well, but users have no way to organise multi-match competitions. Tournaments are the natural next step in helping hosts run real club-style activity.

## In scope (v1)

- Single-elimination bracket only
- 4, 8, or 16 teams (default 8)
- Fixed team size per tournament (e.g. 5 players for football 5)
- Solo sign-ups + team-draft sign-ups, both supported in the same tournament
- Host configures: format, team size, registration window, per-round defaults for date/venue
- Host can re-seed the bracket via drag-swap before round 1 begins
- Host declares the winner of each match (no scores)
- Walkover / forfeit recording
- In-app + email notifications mirroring existing match-invite pattern
- Tournament card appears in the existing event feed
- Tournaments appear in a "Tournaments" tab inside the existing "My Events" screen

## Explicitly out of scope (v1)

- **Double elimination** and **round-robin / group stage** formats (deferred; design room left in the wizard format selector)
- **Score recording** per match — only the winner is captured
- **Public-team join** ("see existing teams and ask to join one") — the only entry paths are solo or captain-led draft
- **Bracket size > 16**
- **Payments / paid tournaments** (tournaments can be marked as paid in the same informational way regular events can, but the platform processes no money)
- **Bracket-side seeding by rating / skill** — initial seeding is random with manual drag-swap

## Personas affected

| Persona | What changes for them |
|---|---|
| **Host** | New action in Host mode: "Create tournament" (wizard). New host-side dashboard for the live bracket. |
| **Player** | New entry path in feed (🏆 cards). New "Tournaments" tab in "My Events". New screens for next match / advanced / eliminated / champion. |
| **Captain** | A subset of Player: when a player starts a team draft they become its captain. Captains can invite, swap, and disband the draft until 5/5 accept. |
| **Admin/mod** | No new tools required for v1. The existing admin powers (edit/delete any event) apply since a tournament is an event subtype. |

## How it fits the existing app

- **Reuses** the existing event feed, search, filtering, and "My Events" tab structure.
- **Reuses** the existing notification infrastructure (`MatchNotificationService`, `ThymeleafMailTemplateRenderer`). New notification types are added; the dual-channel delivery (in-app + email) and per-recipient locale handling are unchanged.
- **Reuses** the `EventJoinPolicy.INVITE_ONLY` pattern as the inspiration for team-draft invites (in-app record + parallel email, accept/decline happens in-app).
- **Reuses** the existing Host mode toggle and create-event shell as the chrome for the create-tournament wizard.
- **Adds** new entities (see `data-model.md`) and a new top-level entity type that wraps a set of matches.
- **Does not** change auth, profiles, reviews, or reporting flows beyond a single new review-trigger point (the tournament completion).

## Done = ?

The feature is considered shipped when:

1. A host can create, publish, seed, run, and complete a full 8-team tournament end-to-end.
2. Players can discover it, join it (both paths), see their next match throughout, and reach a clear post-tournament state.
3. All notifications listed in `notifications.md` are delivered via both channels.
4. The wireframed flows in §1–§7 are implemented matching the picked UI variants (§8B, §9A).

Stretch goals (post-v1):

- Extend wizard format selector to include double elimination
- Per-round skill weighting for auto-seed
- Spectator follow / public bracket share link
