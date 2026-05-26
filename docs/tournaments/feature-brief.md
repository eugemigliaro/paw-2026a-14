# Tournaments Feature Brief

This document is the implementation-facing explanation of the tournaments feature.
Use it as the first source inside `docs/tournaments` when deciding what to build.
The older flow documents still contain useful wireframe details, but this brief
turns the feature into a coherent server-rendered Java/Spring implementation.

## Product Goal

The product currently supports standalone in-person sports matches. Tournaments
add a structured competition mode: a host can organize several teams into a
single-elimination bracket, players can join before registration closes, and the
host advances the bracket by declaring winners.

The feature should feel like part of the same event platform. A tournament is
discoverable like an event, uses the same sports/location/banner language, and
belongs in the same feed. It is not a separate product area and it is not a SPA.

## The Core Model

A tournament is event-like. A bracket fixture is not a standalone match.

That distinction matters:

- `Match` is the existing standalone event that users reserve, join, leave, and
  discover directly.
- `Tournament` is the public event-like aggregate: title, sport, host, location,
  registration window, banner, status, and tournament rules.
- `TournamentMatch` is an internal bracket fixture between `TournamentTeam`
  records. It is controlled by the tournament lifecycle and should not be
  inserted into the existing `matches` table.

Do not implement `TournamentMatch extends Match`.

Normal matches and tournament bracket fixtures are not substitutable. A normal
match has individual participants, capacity, join policies, recurring-series
behavior, participant management, and standalone feed visibility. A tournament
fixture has teams, round/index placement, parent-child propagation, winner
declaration, and no direct reservation flow. Java/JPA inheritance would couple
unrelated lifecycle rules and force invalid nullable fields or guard clauses
through the services, controllers, JSPs, and tests.

Reuse smaller concepts instead:

- sport enum/category
- host user
- title/description
- address and coordinates
- price as informational only
- banner image metadata
- date/time formatting helpers
- email delivery infrastructure
- JSP tags and design tokens

Keep service flows separate:

- `MatchService` continues to own standalone matches.
- `TournamentService` owns tournament CRUD and lifecycle transitions.
- `TournamentRegistrationService` owns solo entries and team drafts.
- `TournamentBracketService` owns bracket generation, scheduling, winner
  declaration, and propagation.
- `TournamentMailService` wraps the existing mail infrastructure for tournament
  emails. A persisted in-app notification center does not exist today and is
  out of scope.

## Recommended V1 Scope

The semester target can include solo signup, captain-led team drafts, bracket
generation, host winner declaration, emails, player states, and My Events
integration. That is too large to implement as one uninterrupted slice.

Build the tournament feature in two layers.

### V1 Spine

The first working version should prove the whole tournament lifecycle with the
fewest moving parts:

- host creates a tournament that opens for registration immediately
- tournament appears in the feed and has a public detail page
- authenticated players join or leave the solo pool
- host manually closes registration
- the system forms teams from solo entries
- host generates the bracket
- host schedules round-one fixtures
- players can view the bracket
- host declares winners
- winners propagate to later rounds
- final winner completes the tournament
- minimum emails are sent for bracket publication, match result,
  cancellation, and completion

This first spine intentionally defers:

- captain-led team drafts
- automatic scheduled transitions
- drag-swap reseeding
- 30-second winner undo
- partial page refresh/live updates
- full email catalog
- polished My Events tournament tab
- bracket sharing
- public completed-tournament browsing

### Expanded V1

After the V1 spine works, add:

- captain-led team drafts and draft invitations
- accept/decline invite screens
- locked-team creation from accepted draft rosters
- feed type filter for tournaments
- My Events tournament tab
- richer player states: next match, advanced, eliminated, champion
- full email catalog
- optional scheduled jobs for close-registration reminders and automatic
  registration close
- host reseeding before bracket publication

This sequencing avoids a giant branch where data, UI, emails, and edge
cases are all half-complete at the same time.

## Lifecycle

Use a lifecycle that is explicit enough for service guards and tests. The old
label `LOCKED_BRACKET` is too vague because it can mean registration closed,
teams fixed, bracket generated, round-one scheduled, or bracket published.

Recommended persisted statuses:

| Status | Meaning |
| --- | --- |
| `REGISTRATION` | Public tournament. Players can join or start drafts. |
| `BRACKET_SETUP` | Registration is closed. Teams are fixed. Host prepares the bracket and round-one schedule. Players do not see unstable seeding. |
| `IN_PROGRESS` | Bracket is public. Host can declare winners and edit future fixture schedules. |
| `COMPLETED` | Final winner declared. Read-only. |
| `CANCELLED` | Tournament cancelled. Read-only except admin/mod deletion. |

Add timestamp fields so the status history is explainable:

- `created_at`
- `updated_at`
- `registration_closed_at`
- `bracket_generated_at`
- `started_at`
- `completed_at`
- `cancelled_at`

The main transitions are:

- `none -> REGISTRATION`: host creates a valid tournament
- `REGISTRATION -> BRACKET_SETUP`: host manually closes registration in the
  first implementation; scheduled auto-close can come later
- `BRACKET_SETUP -> IN_PROGRESS`: host publishes the bracket and round-one
  schedule
- `IN_PROGRESS -> COMPLETED`: final winner is declared
- `REGISTRATION|BRACKET_SETUP|IN_PROGRESS -> CANCELLED`: host or admin cancels

There is no reopen-registration path in the first implementation.

## Product Decisions To Lock For Implementation

These decisions remove ambiguity from the current docs.

| Topic | Decision for first implementation |
| --- | --- |
| Under-capacity close | Auto-cancel when there are fewer complete teams than `bracket_size`. Do not support byes yet. |
| Solo grouping | Group solo entries into complete teams of exactly `team_size`, capped at `bracket_size` teams. Leftover or overflow solo entries are marked unassigned and notified. |
| Team drafts | Defer until the solo-only spine works. Then enforce one active participation path per user per tournament. |
| User accepts invite while in solo pool | When team drafts are added, accepting a draft invite removes the user from the solo pool in the same transaction. |
| User accepts invite while already in another draft/team | Block with a clear service error. No implicit switching between teams. |
| Captain disbands draft | Draft is disbanded and all accepted/pending invitees are notified. No captain promotion in v1. |
| Roster changes after bracket setup | Not allowed. Rosters are locked when registration closes. |
| Per-match cancellation | Do not build a separate cancel-match state. The host records the winner; a forfeit is represented by declaring the non-forfeiting team as winner. |
| Winner undo | Defer player-facing 30-second undo. Use confirmation modal first; admin/mod correction can be a future tool. |
| Live updates | Full page reload after POST is acceptable. Manual refresh is acceptable for player bracket watching. |
| Scores | No scores. The host only declares the winner. |
| Payments | Informational price only. No platform payment processing. |

## Personas And Capabilities

### Host

The host can create, close registration, generate the bracket, schedule
round-one fixtures, publish the bracket, declare winners, cancel
before completion, and complete the tournament through the final result.

The host cannot change format, bracket size, or team size after creation;
reopen registration; reseed after bracket publication; or change a winner
through the normal host UI after declaration.

### Player

The player can discover tournaments, view public details, join or leave the solo
pool during registration, view their team after registration closes, view the
bracket once published, see their next match, keep following after elimination,
and see completed/champion states.

The player cannot join after registration closes, directly join a bracket
fixture, alter their team after registration closes, or declare results.

### Captain

Captain behavior belongs to the expanded implementation, not the first spine.
When added, a captain is a player who starts a team draft, invites existing
users, monitors accept/decline state, and locks a team once the roster reaches
`team_size`.

### Admin/Mod

The first implementation does not need new tournament-specific admin screens.
Existing elevated powers should apply conceptually. Keep authorization in Spring
Security, policy helpers, or services rather than JSP conditionals.

## Data Model Shape

Create separate JPA entities and Flyway migrations for tournament tables.

Recommended aggregate for the first spine:

- `Tournament`
- `TournamentTeam`
- `TournamentTeamMember`
- `TournamentSoloEntry`
- `TournamentMatch`

Add later with team drafts:

- `TournamentTeamDraft`
- `TournamentDraftInvite`

Important persistence rules:

- Do not add tournament bracket fixtures to `matches`.
- Do not make tournament entities extend `Match`.
- Use `@Version` for optimistic locking on rows that can race.
- Enforce obvious uniqueness in the database:
  - one seed position per tournament
  - one user per team
  - one solo entry per user per tournament
  - one bracket fixture per tournament/round/match index
- Enforce cross-table participation exclusivity in
  `TournamentRegistrationService`, and test it heavily.

The venue/location shape should match the current `Match` fields:

- `address`
- `latitude`
- `longitude`

Avoid introducing a vague JSON venue field unless the existing app adopts that
shape first.

For solo grouping, capacity is measured in teams, not players. If a tournament
needs eight teams of five players, only the first forty assignable solo entries
can become bracket teams. Extra solo entries remain outside the bracket with
`UNASSIGNED` status and must receive a clear email.

## Bracket Rules

The first implementation supports only power-of-two single elimination:

- 4 teams -> 3 fixtures
- 8 teams -> 7 fixtures
- 16 teams -> 15 fixtures

Each `TournamentMatch` stores tournament, round number, match index, team A,
team B, winner team, schedule, venue fields, status, and parent fixture links.
Round one receives seeded teams directly. Later rounds start with null team
slots and receive winners through parent-child propagation.

When a winner is declared:

1. Validate the acting user may manage the tournament.
2. Validate tournament status is `IN_PROGRESS`.
3. Validate both teams are known.
4. Validate the selected winner is one of those teams.
5. Persist winner and fixture status.
6. If this fixture has a child, place the winner into the correct child slot.
7. If the child now has both teams, mark it scheduled using round defaults or
   explicit schedule data.
8. If this fixture is the final, complete the tournament.
9. Dispatch result/completion emails after the transaction succeeds.

## UI Structure

This repository is JSP/server-rendered. Treat the component names in the older
docs as JSP tags, JSP fragments, Java view models, and CSS classes, not React
components.

Likely reusable JSP tags/fragments:

- `tournamentBadge.tag`
- `tournamentCard.tag`
- `tournamentLifecycleBadge.tag`
- `bracketGrid.tag`
- `bracketMatchCard.tag`
- `matchFocusRail.tag`
- `teamRosterList.tag`
- `nextTournamentMatch.tag`

Likely views:

- `WEB-INF/views/tournaments/detail.jsp`
- `WEB-INF/views/tournaments/bracket.jsp`
- `WEB-INF/views/host/tournaments/create.jsp`
- `WEB-INF/views/host/tournaments/bracket-setup.jsp`
- `WEB-INF/views/host/tournaments/schedule-round.jsp`
- later: `WEB-INF/views/tournaments/drafts/*.jsp`

Use existing tags like `button.tag`, `card.tag`, `textInput.tag`,
`selectField.tag`, and existing CSS conventions before creating new primitives.
All visible copy must be in both message bundles.

## Emails

The current application has email delivery and email action requests, but it
does not have a persisted in-app notification center or bell dropdown. The
`email_action_requests` table is for token/action flows, not a general
notification inbox. Tournament updates should use emails only.

For the first spine, implement the smallest useful set:

- bracket published / first match scheduled
- match result: won/lost
- tournament cancelled
- tournament completed: champion/participant

Add draft-related emails only when team drafts are implemented.

Do not build digesting or short-window email coalescing unless the existing mail
infrastructure already supports it cleanly. It is a separate subsystem.

## Testing Standard

Tournament work changes domain behavior and must be tested in every layer it
touches.

Minimum expected tests:

- persistence tests for every DAO query and migration shape
- service tests for lifecycle guards
- service tests for registration exclusivity
- service tests for solo grouping and under-capacity cancellation
- service tests for bracket generation validity
- service tests for winner propagation and final completion
- controller tests for public detail, authenticated join, denied host actions,
  and allowed host actions
- locale-aware tests when adding localized UI/mail flows

Use the repository's existing test style:

- JUnit 5 + Mockito for service tests
- Spring Test + HSQLDB for persistence tests
- MockMvc for controller tests
- no `Mockito.verify()` in unit tests
- arrange/exercise/assert structure

## Definition Of Done For The First Spine

The first tournament spine is done when:

1. A host can create a tournament that opens for registration.
2. The tournament appears in discovery and has a public detail page.
3. An authenticated player can join and leave the solo pool.
4. The host can close registration.
5. The system creates complete teams from solo entries or cancels under-capacity
   tournaments.
6. The host can generate a valid single-elimination bracket.
7. The host can publish a round-one schedule.
8. Players can see the bracket after publication.
9. The host can declare winners.
10. Winners propagate correctly until a final champion is produced.
11. The final winner completes the tournament.
12. Required minimum emails are sent.
13. Tests cover allowed and denied paths.
14. All new UI copy exists in English and Spanish.
