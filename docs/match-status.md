# Match Status Notes

This note documents the current behavior of `matches.status` beyond the enum definition in `persistence/src/main/resources/db/migration/V1__init_schema.sql`.

## Current Meaning

- `draft`: hidden work-in-progress state.
- `open`: active match lifecycle state.
- `cancelled`: explicit persisted terminal state.
- `completed`: time-derived lifecycle state in most read paths, not a capacity state.

Important:

- `completed` does not mean "full".
- "Full" is inferred from participant count versus `max_players`.

## How `completed` Is Currently Treated

The main read path derives `completed` in `MatchJdbcDao` with:

- if stored status is `open`
- and `COALESCE(ends_at, starts_at) <= CURRENT_TIMESTAMP`
- then the returned status is `completed`

That means:

- we usually do not update the row from `open` to `completed` when time passes
- we derive `completed` at query time instead
- if `ends_at` is present, it is used as the completion boundary
- if `ends_at` is `NULL`, `starts_at` is used as the completion boundary

## Consequences

- A past `open` match is commonly returned to the app as `completed`.
- Filtering by `completed` in DAO-backed list views includes both:
  - rows explicitly stored as `completed`
  - rows stored as `open` whose time window has already ended
- Reservation logic still treats "reservable" separately from lifecycle status.

## Known Inconsistencies

- The schema allows persisting `completed`, but normal host create/edit flows do not explicitly transition matches into that state.
- A match can still be cancelled after it is already being read as `completed`, because cancellation writes `status = 'cancelled'` without blocking completed matches.
- During the interval after `starts_at` but before `ends_at`, a match may still read as `open`, while reservation service logic rejects new reservations because the event already started.
- The event detail page currently enables reservation from `public + open + available spots`, without also checking whether the match already started.

## Practical Reading Rule

When reasoning about current code, treat status like this:

- `open`: not cancelled and not yet past the derived completion boundary
- `completed`: mostly a derived read-model status based on time
- `cancelled`: explicit stored override
- `full`: separate concept, not part of `match_status`

If this area is refactored later, review `MatchJdbcDao`, `MatchReservationServiceImpl`, `EventController`, and host cancellation flows together so stored status and derived lifecycle behavior stay aligned.
