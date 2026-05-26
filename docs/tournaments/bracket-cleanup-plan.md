# Tournament Bracket Cleanup And Reconciliation Notes

This document originally proposed a narrower `solo-pool + random bracket`
cleanup. During branch reconciliation, the project kept the richer
`merge/tournaments-bracket` behavior for manual/ELO pairing and admin/mod host
controls, while removing walkover support and porting selected UX fixes from
`feat/tournaments`.

## Summary

- Preserve manual/random/ELO pairing and the existing bracket setup page.
- Remove walkover result variants; forfeits are represented by declaring the
  non-forfeiting team as winner.
- Simplify bracket scheduling to one start datetime per match plus a tournament-wide `matchDurationMinutes` configured on create/edit.
- Keep per-match venue/location scheduling, add an optional short public match note, and make those details visible to players on the public bracket.
- Clean up the bracket UI: remove the match-focus rail, make winners obvious, and make `/tournaments/{id}` host/bracket panels match the rest of the site.
- Show the registration-phase participant roster on tournament detail.
- Preserve admin/mod host-equivalent tournament controls for this branch.

## Implementation Changes

- Tournament configuration:
  - Add `matchDurationMinutes` to tournament persistence, model, create/update requests, form validation, and edit flow.
  - Backfill existing tournaments with a default of `90` minutes.
  - Keep the value editable only while the tournament is still editable.

- Bracket scheduling and match data:
  - Change bracket setup to collect per match:
    - required `start datetime`
    - required `venue/address`
    - optional `latitude/longitude` pair
    - optional short public `description` / match note, max `500` chars
  - Stop collecting manual end datetime. Derive `scheduledEndsAt = scheduledStartsAt + matchDurationMinutes`.
  - Keep current schedule ordering checks, but compute them from derived end times.
  - Store the optional match note on `tournament_matches` as a nullable field.

- Result flow:
  - Remove walkover end-to-end:
    - delete the separate `recordWalkover` service/controller route
    - remove `WALKOVER` status and walkover-only failure/copy/mail paths
    - migrate existing persisted `walkover` matches to `done` while preserving `winner_team_id`
  - Keep only “team A won” / “team B won”.
  - Tighten result validation so a winner can only be recorded when:
    - tournament is `IN_PROGRESS`
    - both teams are present
    - no winner is already set
    - `now >= scheduledEndsAt`
  - Keep score out of scope for this slice.

- Solo pool and team naming:
  - Hide/disable team-draft controls from tournament create/edit.
  - Keep bracket pairing fixed to `RANDOM` for this slice and remove pairing-strategy/manual-pairing UI and reachable backend paths.
  - Rename auto-generated solo teams:
    - if `team_size == 1`, team name = player username
    - if `team_size > 1`, use localized generic names `Team {n}` / `Equipo {n}`

- Public and host UI:
  - `/host/tournaments/{id}/bracket/setup`:
    - remove pairing-strategy and manual-pairing blocks
    - remove end date/time fields
    - keep venue/location inputs
    - add optional match note input
  - `/tournaments/{id}/bracket`:
    - remove the “Match focus” sidebar
    - make each match card show schedule and venue directly
    - show the optional note when present
    - if coordinates exist, expose a map/open-location link
    - show the winner clearly on finished matches and visually de-emphasize the loser
  - `/tournaments/{id}`:
    - restyle bracket/host panels to match the rest of the detail page
    - show solo-pool availability in match-style wording during `REGISTRATION` only:
      - `X of Y solo spots left`
      - where `Y = bracket_size * team_size`
      - and `X = Y - active solo-pool entries`
    - do not count team members or formed teams in this label

- Admin/authorization:
  - Restrict tournament host mutations to the real host only.
  - Remove admin/mod override from tournament lifecycle, registration-close, and bracket-mutation services.
  - Change tournament host-page access so admins who are not the host receive `403`, not host-equivalent access.
  - Remove admin-based host controls from tournament detail and bracket pages; admin users can still use public tournament pages and separate admin/moderation features, but not `/host/tournaments/**` flows unless they are the host.

## Public Interfaces / Types

- Extend tournament create/update flows with `matchDurationMinutes`.
- Extend `TournamentMatchScheduleRequest` with optional `description`, and keep `address` plus optional coordinates.
- Remove `recordWalkover(...)` from `TournamentBracketService`.
- Remove walkover-only enum/status/failure surfaces from tournament bracket contracts.
- Extend bracket view models so public match cards can render:
  - venue/address
  - optional note
  - optional map link
  - explicit winner/loser state

## Test Plan

- Service tests:
  - create/update persists valid duration and rejects invalid duration
  - publish derives end time from start + duration
  - result entry before derived end time fails; after derived end time succeeds
  - walkover API/status paths are gone and existing winner propagation still works
  - random pairing is the only supported bracket strategy for this slice
  - solo team naming works for both single-player and multi-player teams
  - multi-player team tournaments still generate, publish, and complete correctly
  - solo-pool availability label values are derived only from active solo entries
  - admin/mod users who are not the host are rejected by tournament mutation services with `FORBIDDEN`

- Web/controller/view tests:
  - create/edit no longer expose team-draft or pairing-strategy UI
  - bracket setup renders start/venue/note fields and no end-time or walkover actions
  - public bracket no longer renders match focus and now renders venue/note details
  - finished matches clearly expose winner state
  - tournament detail shows the solo-pool availability label only in registration state
  - admin/mod users who are not the host get `403` on `/host/tournaments/**` routes and do not see host controls on public tournament pages

- Persistence/migration tests:
  - migration adds `match_duration_minutes` and per-match description
  - migration converts existing `walkover` rows to `done` without losing winner data
  - updated constraints still allow required address and optional coordinate pairs

## Assumptions

- Separate public bracket and host bracket-setup pages remain; this slice simplifies them instead of merging them into `/tournaments/{id}`.
- The optional match note is public to all bracket viewers.
- Venue/address stays per match; it is not forced to inherit from the tournament-level venue.
- Solo-pool availability is shown only while registration is open or not yet closed; it disappears once the tournament moves out of `REGISTRATION`.
- Score entry remains explicitly deferred.
- The admin restriction is tournament-only; existing non-tournament host/moderation behavior stays unchanged in this slice.
