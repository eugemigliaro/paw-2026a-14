# Tournaments — Data Model

> Implementation note: read [`feature-brief.md`](./feature-brief.md) and
> [`implementation-plan.md`](./implementation-plan.md) first. This document is
> still useful for entity relationships, but the newer docs clarify the first
> implementation spine and the decision not to model tournament fixtures as
> `Match` rows or subclasses.

This document outlines the new entities, their relationships to the existing schema, and key fields. Names use the project's `snake_case` SQL / `camelCase` Java conventions and follow Flyway migration numbering.

Enum names below refer to Java constants. Persisted database enum values should
follow the existing project convention: lowercase strings and lower snake case
for multi-word values.

> **Note:** Field-level types and constraints below are recommendations, not prescriptions — the implementer should reconcile with the existing `Match`, `MatchParticipant`, `User`, and `Series` models before committing the schema. The shapes are designed to integrate with the existing `MatchParticipationService` and `MatchNotificationService`.

## Entity overview

```
                       +--------------+
                       |    User      |
                       +--------------+
                              ^
                              |  (host)
                              |
                       +--------------+         +-------------------+
                       |  Tournament  | ──────> | TournamentMatch   |
                       +--------------+ 1     n +-------------------+
                              ^                          |
                              |                          |  team_a / team_b / winner
                              |                          v
                              | 1                +-------------------+
                              +────────────────> |  TournamentTeam   |
                                              n  +-------------------+
                                                          ^
                                                          | 1
                                                          |
                                                  n       v
                                                +-------------------------+
                                                |  TournamentTeamMember   |
                                                +-------------------------+
                                                          ^
                                                          |   (user)
                                                          v
                                                  +--------------+
                                                  |    User      |
                                                  +--------------+

           +------------------------+
           |  TournamentSoloEntry   |  (player joined solo, not yet on a team)
           +------------------------+

           +------------------------+
           |  TournamentTeamDraft   |  (captain-led draft before team locks)
           +------------------------+
                   ^
                   | 1
                   v
           +------------------------+
           | TournamentDraftInvite  |  (pending / accepted / declined invites)
           +------------------------+
```

The shape mirrors the existing `Match → MatchParticipant → User` pattern; treat `TournamentTeam` as the participation unit (the equivalent of an individual `MatchParticipant`) and `TournamentTeamMember` as the per-user record on that team.

## Tournament

The top-level entity. One row per tournament.

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| host_user_id | BIGINT FK → User | Required. |
| title | VARCHAR(120) | Required. |
| description | TEXT | Optional. |
| sport_category_id | BIGINT FK | Reuse the existing event-category foreign key. |
| skill_level | ENUM | `BEGINNER` / `INTERMEDIATE` / `ADVANCED`, reuse existing enum. |
| banner_url | TEXT | Optional. Same upload pipeline as event banners. |
| format | ENUM | `SINGLE_ELIMINATION` only for v1; column should exist for future formats. |
| bracket_size | SMALLINT | 4, 8, or 16 (CHECK constraint). |
| team_size | SMALLINT | Number of players per team. |
| allow_solo_signup | BOOLEAN | Default `TRUE`. |
| allow_team_draft | BOOLEAN | Default `TRUE`. At least one must be true. |
| registration_opens_at | TIMESTAMPTZ | When players can start signing up. |
| registration_closes_at | TIMESTAMPTZ | Auto-transition trigger to `LOCKED_BRACKET`. |
| status | ENUM | `DRAFT`, `REGISTRATION`, `LOCKED_BRACKET`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. |
| default_venue | TEXT or JSONB | Reuse the existing event location structure. |
| price | NUMERIC(10,2) | Nullable. Informational only (no payment processing). |
| created_at / updated_at | TIMESTAMPTZ | |

## TournamentTeam

One row per team that takes a slot in the bracket. A team is created when:
- A team draft reaches `team_size` accepted invites and locks in, **OR**
- The host or the auto-bundling job groups solo-pool entries into a synthetic team during the `LOCKED_BRACKET` transition.

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| tournament_id | BIGINT FK | |
| name | VARCHAR(80) | Captain-chosen, or synthesized for solo-pool teams (`"Solo squad #1"`). |
| origin | ENUM | `TEAM_DRAFT` or `SOLO_POOL`. |
| seed_position | SMALLINT | 1-indexed position in the bracket. Nullable until bracket generated. |
| created_at | TIMESTAMPTZ | |

Indexes: `(tournament_id, seed_position)` unique when `seed_position` is not null.

## TournamentTeamMember

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| team_id | BIGINT FK | |
| user_id | BIGINT FK → User | |
| is_captain | BOOLEAN | True for the player who started the team draft. False for everyone else. Always false for SOLO_POOL teams. |
| joined_at | TIMESTAMPTZ | |

Constraint: `(team_id, user_id)` UNIQUE. A user can be in at most one `TournamentTeam` per tournament — enforce in service layer.

## TournamentMatch

One row per match in the bracket. For an 8-team single-elim tournament there are 7 matches: 4 quarter-finals + 2 semi-finals + 1 final.

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| tournament_id | BIGINT FK | |
| round | SMALLINT | 1 = round 1 (quarters in an 8-team), 2 = semis, 3 = final. |
| match_index | SMALLINT | 0-indexed position within the round. |
| team_a_id | BIGINT FK → TournamentTeam | Nullable until propagated from previous round. |
| team_b_id | BIGINT FK → TournamentTeam | Nullable. |
| winner_team_id | BIGINT FK → TournamentTeam | Nullable until host declares. |
| scheduled_at | TIMESTAMPTZ | Nullable, defaults from `Tournament` per-round defaults. |
| venue | TEXT or JSONB | Same shape as Tournament.default_venue. |
| status | ENUM | `PENDING` (team(s) not known) / `SCHEDULED` / `AWAITING_RESULT` (past scheduled time, no winner) / `WALKOVER` / `DONE`. |
| parent_match_a_id | BIGINT FK self | The round-N–1 match whose winner becomes team_a. NULL for round 1. |
| parent_match_b_id | BIGINT FK self | Same for team_b. |
| created_at / updated_at | TIMESTAMPTZ | |

When a match's winner is set, the corresponding `team_?_id` of the child match is updated via the `parent_match_?_id` link.

## TournamentTeamDraft

The state of an in-progress team draft (before it has locked). Once the draft reaches `team_size` accepted invites, a `TournamentTeam` is created from this draft and the draft is either deleted or kept as historical record (recommend: kept, with a `locked_at` timestamp and FK to the created team).

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| tournament_id | BIGINT FK | |
| captain_user_id | BIGINT FK → User | |
| name | VARCHAR(80) | |
| status | ENUM | `OPEN` (collecting invites), `LOCKED` (became a team), `DISBANDED` |
| created_team_id | BIGINT FK | Nullable, set when status flips to LOCKED. |
| created_at | TIMESTAMPTZ | |
| locked_at | TIMESTAMPTZ | Nullable. |

## TournamentDraftInvite

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| draft_id | BIGINT FK | |
| invited_user_id | BIGINT FK → User | |
| status | ENUM | `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED` (captain swapped them out). |
| invited_at | TIMESTAMPTZ | |
| responded_at | TIMESTAMPTZ | Nullable. |

Constraint: `(draft_id, invited_user_id)` UNIQUE while status in (`PENDING`, `ACCEPTED`).

## TournamentSoloEntry

One row per user who joined the solo pool.

| Field | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| tournament_id | BIGINT FK | |
| user_id | BIGINT FK → User | |
| status | ENUM | `IN_POOL`, `LEFT`, `ASSIGNED` (when bundled into a SOLO_POOL team). |
| assigned_team_id | BIGINT FK → TournamentTeam | Nullable, set when bundled. |
| joined_at | TIMESTAMPTZ | |

Constraint: `(tournament_id, user_id)` UNIQUE.

Cross-entity service-layer rule (enforce in `TournamentRegistrationService`):

> A user may have at most one of `TournamentSoloEntry`, `TournamentDraftInvite (status ACCEPTED)`, or `TournamentTeamMember` per tournament.

## Mapping to existing tables

You will **not** reuse the `Match` table for tournament matches. Tournament matches are conceptually a different kind of entity (they have parent-match propagation, no separate join/reservation flow, etc.) and trying to overload `Match` will cost more than it saves.

You **may** reuse:

- `EventCategory` and the existing skill-level enum for `Tournament.sport_category_id` and `skill_level`.
- The existing location / venue JSONB shape.
- The existing banner-image upload pipeline.
- `MatchNotificationService` infrastructure (extend with new methods; see `notifications.md`).
- The `ThymeleafMailTemplateRenderer` pattern (add new render methods for each notification type).

## Suggested Flyway migrations

```
V20__create_tournament_table.sql
V21__create_tournament_team_and_members.sql
V22__create_tournament_match_table.sql
V23__create_tournament_draft_and_invites.sql
V24__create_tournament_solo_entry.sql
V25__add_tournament_indexes.sql
```

## Suggested service layer

| Service | Responsibility |
|---|---|
| `TournamentService` | CRUD + status transitions + lifecycle guards |
| `TournamentRegistrationService` | Solo pool, team drafts, invite send/accept/decline, cross-entity "one membership per user per tournament" |
| `TournamentBracketService` | Generate, re-seed, swap, schedule rounds, propagate winners |
| `TournamentNotificationService` | New notification types — wraps `MatchNotificationService` infrastructure |

Keep `Tournament` and `Match` services independent — there is no `Tournament extends Match` inheritance, and conflating them in a base class will produce more friction than reuse.
