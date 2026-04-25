# Recurring Events Sprint Brief

## Working Assumption

Use a recurring-event model that fits the existing codebase instead of cloning controller flows for each case:

- Keep current single-match behavior as the baseline.
- Add a recurrence parent concept plus concrete occurrences that can be individually joined, edited, or cancelled.
- Treat series-level actions as bulk operations over future occurrences.
- Treat single-occurrence actions as occurrence-specific overrides or exceptions.
- Reuse current entry points first: `HostController`, `EventController`, `MatchService`, `MatchReservationService`, `MatchParticipationService`, `MatchNotificationService`, `MatchDao`, `MatchParticipantDao`, `CreateEventForm`, `host/create-match.jsp`, `matches/detail.jsp`, both message bundles, service tests, DAO tests, and `MockMvc` tests where routes change.

## Ticket Estimates

- `PAW2026A14-145` Host: Choose recurrence frequency -> `3`
- `PAW2026A14-146` Host: Choose recurrence end condition -> `3`
- `PAW2026A14-144` Host: Create recurring match -> `8`
- `PAW2026A14-151` Player: Join recurring event series -> `8`
- `PAW2026A14-154` Player: Leave entire recurring series -> `5`
- `PAW2026A14-147` Host: Edit single occurrence of recurring event -> `8`
- `PAW2026A14-149` Host: Cancel single occurrence only -> `5`
- `PAW2026A14-148` Host: Edit all future occurrences of recurring event -> `13`
- `PAW2026A14-150` Host: Cancel recurring series -> `5`
- `PAW2026A14-152` Player: Join single occurrence only -> `8`
- `PAW2026A14-153` Player: Leave single occurrence only -> `5`
- `PAW2026A14-155` Player: Receive notification about joined recurring match update -> `8`

## Recommended Implementation Order

1. `PAW2026A14-145` Host: Choose recurrence frequency
2. `PAW2026A14-146` Host: Choose recurrence end condition
3. `PAW2026A14-144` Host: Create recurring match
4. `PAW2026A14-151` Player: Join recurring event series
5. `PAW2026A14-154` Player: Leave entire recurring series
6. `PAW2026A14-147` Host: Edit single occurrence of recurring event
7. `PAW2026A14-149` Host: Cancel single occurrence only
8. `PAW2026A14-148` Host: Edit all future occurrences of recurring event
9. `PAW2026A14-150` Host: Cancel recurring series
10. `PAW2026A14-152` Player: Join single occurrence only
11. `PAW2026A14-153` Player: Leave single occurrence only
12. `PAW2026A14-155` Player: Receive notification about joined recurring match update

## Codex-Ready Briefs

### `PAW2026A14-145` - Host: Choose recurrence frequency

- Difficulty: `3`
- Goal: extend the host form so a match can optionally repeat on a supported cadence.
- Likely files/layers:
  - `webapp/src/main/java/.../form/CreateEventForm.java`
  - `webapp/src/main/java/.../controller/HostController.java`
  - `webapp/src/main/webapp/WEB-INF/views/host/create-match.jsp`
  - `webapp/src/main/resources/i18n/messages.properties`
  - `webapp/src/main/resources/i18n/messages_es.properties`
- What to implement:
  - Add recurrence fields such as `isRecurring`, `frequency`, and any minimal interval fields explicitly required by the ticket.
  - Keep the non-recurring create flow unchanged.
  - Validate only allowed frequency values.
  - Keep business rules out of the JSP.
- Done when:
  - The create form can capture a recurrence frequency.
  - Invalid values produce localized validation errors.
  - A normal non-recurring match can still be created with no behavior change.
- Testing:
  - Form binding and validation tests.
  - UI rendering of English and Spanish labels.
  - Regression coverage for non-recurring submission.

### `PAW2026A14-146` - Host: Choose recurrence end condition

- Difficulty: `3`
- Goal: let the host define when recurrence stops.
- Likely files/layers:
  - Same host create flow as the previous ticket.
  - Service request DTOs if recurrence settings are already passed downward.
- What to implement:
  - Add end-mode inputs such as `untilDate`, `occurrenceCount`, or equivalent based on the ticket.
  - Validate mutually exclusive states.
  - Reject impossible schedules.
  - Preserve locale-aware validation messages.
- Done when:
  - The host can choose how the series ends.
  - Invalid combinations are rejected clearly.
  - Recurrence data is ready to be consumed by creation logic.
- Testing:
  - One happy path per end mode.
  - One invalid mixed-state case.
  - Both locales covered.

### `PAW2026A14-144` - Host: Create recurring match

- Difficulty: `8`
- Goal: persist a recurring series and generate occurrences from the host form.
- Likely files/layers:
  - `persistence/src/main/resources/db/migration/...`
  - `service-contracts/src/main/java/.../CreateMatchRequest.java`
  - `service-contracts/src/main/java/.../MatchService.java`
  - `services/src/main/java/.../MatchServiceImpl.java`
  - `persistence-contracts/src/main/java/.../MatchDao.java`
  - `persistence/src/main/java/.../MatchJdbcDao.java`
  - Host create controller flow and detail rendering
- What to implement:
  - Introduce recurrence persistence.
  - Create the parent series and generated occurrences transactionally.
  - Decide what canonical detail page should represent for recurring events.
  - Render at least the next occurrence and/or schedule list on the detail page.
- Done when:
  - A recurring match can be created from the existing host flow.
  - Occurrences are persisted and recoverable from the service/DAO layers.
  - The created item has a stable redirect target and usable detail page.
- Testing:
  - Service test for recurrence generation rules.
  - DAO integration test for inserted series and occurrences.
  - Web test for successful recurring creation and redirect.

### `PAW2026A14-151` - Player: Join recurring event series

- Difficulty: `8`
- Goal: let a user join all eligible future occurrences in one action.
- Likely files/layers:
  - `webapp/src/main/java/.../controller/EventController.java`
  - `service-contracts/src/main/java/.../MatchReservationService.java`
  - `services/src/main/java/.../MatchReservationServiceImpl.java`
  - `persistence-contracts/src/main/java/.../MatchParticipantDao.java`
  - `persistence/src/main/java/.../MatchParticipantJdbcDao.java`
  - `webapp/src/main/webapp/WEB-INF/views/matches/detail.jsp`
- What to implement:
  - Add a series-level call to action.
  - Bulk-create reservations for future eligible occurrences only.
  - Define deterministic handling for started, full, cancelled, or already joined occurrences.
  - Reuse current reservation rules where possible.
- Done when:
  - A player can join the future series in one action.
  - Duplicate or invalid joins are rejected predictably.
  - UI state reflects joined-series membership.
- Testing:
  - Happy path bulk join.
  - Duplicate join rejection.
  - Capacity and started-occurrence edge cases.

### `PAW2026A14-154` - Player: Leave entire recurring series

- Difficulty: `5`
- Goal: let a user cancel their future reservations across the whole series.
- Likely files/layers:
  - Reservation or participation service layer
  - Participant DAO bulk cancellation path
  - Event detail or dashboard entry point
- What to implement:
  - Bulk-cancel future active reservations only.
  - Leave past attendance untouched.
  - Prefer status changes or explicit cancellation tracking over destructive deletion if the current schema patterns support it.
- Done when:
  - The user can leave the future series in one action.
  - Capacity is released correctly.
  - Past occurrences remain historically intact.
- Testing:
  - Future reservations cancelled.
  - Past reservations untouched.
  - UI state updated correctly after leaving.

### `PAW2026A14-147` - Host: Edit single occurrence of recurring event

- Difficulty: `8`
- Goal: allow one occurrence to diverge from the series defaults.
- Likely files/layers:
  - Host edit route and occurrence-specific route if needed
  - Update request DTOs
  - `MatchService.updateMatch(...)` or a new occurrence-update path
  - DAO update logic
  - Detail or schedule rendering
- What to implement:
  - Introduce an occurrence override model for ticket-approved fields.
  - Keep the rest of the series unchanged.
  - Make it explicit in the UI that the edit applies to one date only.
- Done when:
  - Editing one occurrence changes only that occurrence.
  - The rest of the series retains parent/default values.
  - Users can tell in the UI which date was edited.
- Testing:
  - Single occurrence updated.
  - Sibling occurrences unchanged.
  - Validation behaves correctly for occurrence-level edits.

### `PAW2026A14-149` - Host: Cancel single occurrence only

- Difficulty: `5`
- Goal: cancel one date in the series without cancelling the rest.
- Likely files/layers:
  - Host manage actions
  - Service cancel path
  - DAO update logic
  - Detail and schedule UI
- What to implement:
  - Mark only the selected occurrence as cancelled.
  - Disable join actions for that date.
  - Keep the series and sibling occurrences active.
  - Reuse current single-match cancellation semantics where possible.
- Done when:
  - The chosen occurrence is clearly cancelled.
  - Players cannot join that occurrence.
  - The rest of the series still works normally.
- Testing:
  - Cancelled occurrence not reservable.
  - Other occurrences unchanged.
  - UI exposes cancelled state correctly.

### `PAW2026A14-148` - Host: Edit all future occurrences of recurring event

- Difficulty: `13`
- Goal: apply changes from a chosen occurrence onward.
- Likely files/layers:
  - Host edit flow
  - Service layer recurrence update logic
  - DAO bulk update logic
  - Schedule rendering
  - Notification hooks if edits should fan out later
- What to implement:
  - Implement a split-point update strategy.
  - Leave past occurrences unchanged.
  - Update the selected occurrence and future occurrences.
  - Decide how existing one-off overrides or cancellations should behave.
- Done when:
  - Future-only propagation works from a selected pivot occurrence.
  - Past dates remain unchanged.
  - Override handling is intentional and documented in code/tests.
- Testing:
  - Past untouched, future updated.
  - Capacity/schedule validation still enforced.
  - Already overridden or cancelled future occurrences handled consistently.

### `PAW2026A14-150` - Host: Cancel recurring series

- Difficulty: `5`
- Goal: cancel the active future part of the whole series.
- Likely files/layers:
  - Host manage actions
  - `MatchService` cancellation path or a new series-specific cancellation path
  - DAO bulk update logic
  - Detail/dashboard state rendering
- What to implement:
  - Bulk-cancel future occurrences of the series.
  - Preserve past or completed occurrences.
  - Make the series visibly inactive for future participation.
- Done when:
  - Future occurrences are cancelled in bulk.
  - No new joins are allowed for cancelled future dates.
  - The host/player UI reflects the cancelled-series state.
- Testing:
  - Future occurrences cancelled.
  - Past occurrences preserved.
  - UI and service behavior consistent after cancellation.

### `PAW2026A14-152` - Player: Join single occurrence only

- Difficulty: `8`
- Goal: let a user join one date without joining the full series.
- Likely files/layers:
  - Detail schedule UI
  - Reservation service
  - Participant DAO
  - Occurrence lookup and availability checks
- What to implement:
  - Add occurrence-specific join actions.
  - Ensure they coexist with series-level membership rules.
  - Hide or disable conflicting actions when the player already joined the full series.
- Done when:
  - The player can join one occurrence only.
  - Duplicate or conflicting states are handled cleanly.
  - Availability rules remain enforced.
- Testing:
  - Single occurrence join happy path.
  - Duplicate rejection.
  - Capacity and started-occurrence checks.

### `PAW2026A14-153` - Player: Leave single occurrence only

- Difficulty: `5`
- Goal: let a user leave one date only.
- Likely files/layers:
  - Same occurrence-level participation surface as the previous ticket
  - Reservation/participation service logic
  - DAO cancellation or skip override support
- What to implement:
  - Support leaving a one-off joined occurrence.
  - If the product requires it, support skipping one occurrence from an otherwise joined series.
  - Prefer an explicit skip or override record over a naive delete if series membership is stored separately.
- Done when:
  - One occurrence can be left without affecting other future dates.
  - Capacity is released correctly.
  - UI clearly shows the left or skipped occurrence.
- Testing:
  - One-date leave frees capacity.
  - Other reservations remain intact.
  - Series membership behaves correctly after a single-date leave.

### `PAW2026A14-155` - Player: Receive notification about joined recurring match update

- Difficulty: `8`
- Goal: notify affected players when a joined recurring event changes.
- Likely files/layers:
  - `service-contracts/src/main/java/.../MatchNotificationService.java`
  - `services/src/main/java/.../MatchNotificationServiceImpl.java`
  - Mail templates under `services/src/main/resources/mail`
  - Message bundles
  - Edit/cancel service flows that should trigger notification
- What to implement:
  - Define notification triggers first:
    - single occurrence edited
    - future occurrences edited
    - single occurrence cancelled
    - whole series cancelled
  - Notify only affected players.
  - Avoid notifying unrelated series participants when only one occurrence changed.
- Done when:
  - Impacted players receive one update notification per relevant change.
  - Non-affected players are excluded.
  - Notification hooks are attached to the final stable edit/cancel flows.
- Testing:
  - Service tests for recipient selection.
  - One mail per affected participant.
  - Non-affected participants excluded.

## Cross-Ticket Rules For Codex

- Keep controllers thin and move recurrence rules into services.
- Add every visible string to both `messages.properties` and `messages_es.properties`.
- Preserve locale across redirects and mail links.
- Prefer extending `MatchService` and `MatchReservationService` over controller-side branching.
- Add DAO tests for every new recurrence query or update path.
- Add service tests for bulk operations and edge cases.
- Add route or UI tests when new host/player actions are exposed.
- Run at least `mvn test`.

## Why This Order

- The first three tickets establish the recurrence model and creation flow.
- The next two add the simplest usable player behavior at series level.
- Host occurrence exceptions come next because several later tickets depend on them.
- Player single-occurrence join and leave rely on stable occurrence identity and exception handling.
- Notifications should be attached after edit and cancel behavior is stable.
