# Tournament Bracket Cleanup And Reconciliation Notes

This document tracks the reconciliation between `feat/tournaments` and
`merge/tournaments-bracket`. The branch now keeps the richer bracket work from
`merge/tournaments-bracket` and ports only the compatible fixes from
`feat/tournaments`.

## Reconciled State

- Preserve manual/random/ELO pairing and the existing bracket setup page.
- Preserve admin/mod host-equivalent tournament controls from this branch.
- Remove walkover result variants. A forfeit is handled by declaring the
  non-forfeiting team as the winner.
- Keep tournament match scheduling as explicit start/end datetimes per bracket
  fixture.
- Keep score entry out of scope.
- Reuse the match-style venue/location picker behavior on tournament create and
  edit.
- Reuse the existing image pipeline for optional tournament banner uploads.
- Show a participant roster during `REGISTRATION`.
- Warn the host before closing registration when the current participant set
  would cancel the tournament.
- Avoid redundant solo-pool join/leave success flashes.
- Ensure My Events can show tournaments immediately after switching to the
  tournament event type.

## Implemented In This Reconciliation

- Removed `WALKOVER` from tournament match status and service/controller/mail
  surfaces.
- Added migrations that convert any existing persisted `walkover` matches to
  `done` and recreate the tournament match status constraint without
  `walkover`.
- Added `bannerImage` handling to the tournament create/update flow using the
  existing `ImageService`.
- Added tournament participant rows to the detail view model and rendered the
  roster while registration is open.
- Added registration-readiness data so the close-registration action can ask for
  confirmation when it will cancel the tournament.
- Updated the shared submit guard to support per-form confirmation messages.
- Removed success flashes for solo-pool join/leave; the persistent entry state
  is the source of truth.
- Fixed My Events tournament filter generation so match-only date/status filters
  do not hide tournaments until another filter is clicked.
- Updated tournament docs to describe the no-walkover result model.

## Verified Flows

- Clean local database migration from scratch.
- Host login with bootstrap admin.
- Tournament creation with venue/location fields and banner upload.
- Registration detail with solo entry and participant roster.
- Invalid close-registration confirmation.
- My Events tournament filter initial render.
- Valid registration close for a four-team solo tournament.
- Bracket setup open, bracket generation, schedule publication, public bracket
  viewing, winner recording, and tournament completion.

## Remaining Cleanup Units

### Unit 1: Bracket Setup UX

- Make the bracket setup page visually consistent with the rest of the webapp.
- Reduce the repeated "generate/publish" screen feel after a bracket is already
  generated.
- Make validation summary and schedule inputs denser and easier to scan.
- Keep the existing pairing strategy behavior unless the product decision
  changes.

### Unit 2: Public Bracket Polish

- Make winners and losers more visually obvious on completed matches.
- Revisit the match-focus side rail; either make it useful or remove it.
- Ensure public bracket cards show schedule and venue in a compact,
  match-card-like layout.

### Unit 3: Tournament Detail Panels

- Bring host action, entry status, bracket link, and participant roster panels
  closer to the match detail page visual language.
- Replace duplicated closed/in-progress entry text with a single concise state.
- Show solo-pool capacity during registration if product wants a capacity label.

### Unit 4: Bracket Scheduling Enhancements

- Decide whether to keep explicit start/end datetimes per match or introduce a
  tournament-level default duration.
- If default duration is added, migrate schema and derive match end times from
  start time plus duration.
- If per-match notes are added, expose them on both setup and public bracket.

### Unit 5: Team Draft Expansion

- Team drafts remain broader-scope work.
- Preserve one active participation path per user per tournament.
- Add draft invitations, captain flows, draft locking, and related emails only
  after the solo/bracket lifecycle remains stable.

## Test Focus For Future Units

- Service tests for any new scheduling or result-validation rules.
- Controller tests for host/admin authorization on tournament mutations.
- View/controller tests for participant roster, entry states, and bracket setup
  pages.
- Browser smoke for create -> register -> close -> generate -> publish ->
  declare winners whenever the bracket pages change.
