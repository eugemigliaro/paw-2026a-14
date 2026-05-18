# Tournaments Feature — Implementation Guide

This folder is the source of truth for building the Tournaments feature. It captures the decisions reached during wireframe planning so that whoever implements (Claude Code or human) can do so without re-litigating product questions.

Read [`overview.md`](./overview.md) first.

## TL;DR

A Host can publish a **single-elimination bracket tournament** of **8 teams** (configurable to 4 or 16). Players join either **solo** (going into a pool the Host later groups into teams) or as part of a **team draft** (a captain invites N–1 friends, the team only locks once 5/5 accept). When registration closes, the Host generates the bracket, can drag-swap seeds, schedules each round, and then declares winners one match at a time — there is **no score handling**, only winner declaration. Tournaments live inside the existing event feed with a 🏆 badge.

## File index

| File | What it covers |
|---|---|
| [`overview.md`](./overview.md) | Feature scope, goals, non-goals, how it fits the existing app |
| [`lifecycle.md`](./lifecycle.md) | The 5 phases every tournament moves through + transitions |
| [`data-model.md`](./data-model.md) | New entities, relationships, suggested schema |
| [`notifications.md`](./notifications.md) | Dual-channel (email + in-app) pattern + notification catalog |
| [`ui-patterns.md`](./ui-patterns.md) | Picked UI variants + design-system mapping |
| [`open-questions.md`](./open-questions.md) | Edge cases & decisions still pending |
| [`flows/host.md`](./flows/host.md) | Host flows: create → seed bracket → run tournament |
| [`flows/user-joining.md`](./flows/user-joining.md) | Discover → join solo OR start team draft |
| [`flows/player-during-play.md`](./flows/player-during-play.md) | Player views once the bracket is live |

## Key decisions at a glance

1. **Format:** single-elimination only for v1. Double-elim and round-robin explicitly deferred.
2. **Bracket size:** medium (4 / 8 / 16). Default 8.
3. **Team formation:** users join solo, OR a captain starts a team draft and invites others. No "join an existing public team" path.
4. **Team draft lock:** a team takes a bracket slot only when its roster reaches `team_size` accepted invites. Until then it occupies no slot.
5. **Tournament as event type:** tournaments live in the existing event feed with a `🏆 TOURNAMENT` badge, not in a separate tab. Card uses the same chrome as a regular event.
6. **Scores:** none. Host only declares a winner per match.
7. **Authentication:** no email-link confirmations (the platform now has proper auth). Joins are immediate; notifications are sent in parallel via email + in-app.
8. **Bracket UI:** "bracket + match focus" — bracket on the left, a context rail on the right showing the selected match.
9. **Re-seeding window:** Host can drag-swap seeds only before round 1 starts.
10. **Lifecycle:** strict phases (`DRAFT → REGISTRATION → LOCKED · BRACKET → IN PROGRESS → COMPLETED`). Each phase changes which actions are permitted.

## Reference

The interactive wireframes that produced this spec are in `Tournaments Wireframes.html` (storyboard canvas with 9 sections). Whenever a flow doc says "see §N", it means that numbered section in the wireframes.
