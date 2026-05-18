# Tournaments Feature - Implementation Guide

This folder documents the tournaments feature. Start with the two canonical
implementation documents below, then use the older flow documents for detailed
wireframe behavior.

## Start Here

| File | Purpose |
| --- | --- |
| [`feature-brief.md`](./feature-brief.md) | Corrected, implementation-facing explanation of the feature and key technical decisions. |
| [`implementation-plan.md`](./implementation-plan.md) | Detailed staged backlog for building tournaments in this repository. |

## TL;DR

A tournament is an event-like aggregate hosted by a user. Players register
before the window closes, teams are locked, a single-elimination bracket is
generated, and the host advances the bracket by declaring winners or walkovers.
There is no score handling in v1.

The important architecture decision is that bracket fixtures are not normal
matches:

- `Match` remains the standalone event users reserve/join directly.
- `Tournament` is the public event-like container.
- `TournamentMatch` is an internal bracket fixture between tournament teams.
- `TournamentMatch` must not reuse the `matches` table and must not extend
  `Match`.

Build the feature incrementally. The first working spine should support host
creation, feed/detail visibility, solo registration, manual registration close,
team formation, bracket generation, round-one scheduling, winner propagation,
completion, and minimum email notifications. Team drafts, drag reseeding, scheduled
automation, full notification catalog, and polished My Events integration should
come after that spine works.

## File Index

| File | What it covers |
| --- | --- |
| [`feature-brief.md`](./feature-brief.md) | Canonical feature explanation, corrected decisions, recommended first scope |
| [`implementation-plan.md`](./implementation-plan.md) | Detailed phase-by-phase implementation checklist |
| [`overview.md`](./overview.md) | Original feature scope, goals, non-goals, and app fit |
| [`lifecycle.md`](./lifecycle.md) | Original lifecycle write-up; prefer `feature-brief.md` for implementation status names |
| [`data-model.md`](./data-model.md) | Original entity and schema recommendations |
| [`notifications.md`](./notifications.md) | Email-first notification catalog and future in-app notification note |
| [`ui-patterns.md`](./ui-patterns.md) | UI variants and design-system mapping; adapt component names to JSP tags/fragments |
| [`open-questions.md`](./open-questions.md) | Edge cases and unresolved/future decisions |
| [`flows/host.md`](./flows/host.md) | Host flows: create, seed bracket, run tournament |
| [`flows/user-joining.md`](./flows/user-joining.md) | Discover, join solo, and team-draft paths |
| [`flows/player-during-play.md`](./flows/player-during-play.md) | Player views after the bracket is live |

## Key Implementation Decisions

1. Format: single elimination only for the first implementation.
2. Bracket size: 4, 8, or 16 teams.
3. First spine: solo registration only. Add captain-led team drafts after the
   bracket lifecycle works end to end.
4. Scores: none. Host declares winner or walkover only.
5. Lifecycle statuses for new code: `DRAFT`, `REGISTRATION`, `BRACKET_SETUP`,
   `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
6. Public tournament detail URL: `/tournaments/{id}`.
7. Under-capacity registration close: cancel the tournament for v1; do not
   implement byes.
8. Roster changes after registration close: not allowed.
9. Winner undo: defer. Use confirmation first; admin correction can be designed
   later.
10. UI: server-rendered JSP with reusable tags/fragments, not React components.
11. Notifications: implement minimum essential tournament emails first. A
    persisted in-app notification center does not exist today and should be a
    separate foundation if required later.

## Reference

The interactive wireframes that produced the original spec are in
`Tournaments Wireframes.html` if present in the project materials. Whenever a
flow doc says "see section N", it refers to that storyboard.
