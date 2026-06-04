# Test Mutable Holder Refactor Plan

## Summary

Sprint correction:

> Tests use `AtomicReference`, `AtomicBoolean`, or similar holders to verify mocked method calls. This tests implementation details instead of behavior.

Goal: remove or refactor all test code that uses mutable holders to prove that a mocked method or callback was invoked.

This plan intentionally avoids:

- production code changes
- weakening meaningful coverage
- replacing `AtomicBoolean` with another call-proof workaround
- adding `Mockito.verify(...)` or `Mockito.never(...)`
- adding `ArgumentCaptor`

Preferred replacement: assert observable behavior such as returned values, model/flash output, persisted fake state, domain state transitions, mail/event boundary output, or controlled failure behavior.

## Findings

Searches performed across `*/src/test/**`:

- `AtomicReference`, `AtomicBoolean`, `AtomicInteger`, `AtomicLong`
- `doAnswer`, `thenAnswer`, `Answer`
- mutable array holder patterns
- custom holder/captor/flag classes
- mutable collections used to capture invocation arguments
- `Mockito.verify(...)` and `verify(...)`

Results:

- Problematic `Atomic*` or mutable invocation holders are concentrated in service and webapp tests.
- No mutable array holder patterns were found.
- No custom holder/captor/flag classes were found.
- No existing `Mockito.verify(...)` or `verify(...)` usages were found.
- Some `thenAnswer(invocation -> invocation.getArgument(...))` stubs are benign identity stubs and do not need removal unless they mutate holders.
- Some `AtomicLong` usages are fake ID generators, not call verification. They should still be removed so the final scan has no `Atomic*` in tests.

## Affected Files

### `services/src/test/java/ar/edu/itba/paw/services/AccountAuthServiceImplTest.java`

Affected tests:

- `testRegisterCreatesUnverifiedAccountAndSendsVerificationMail`
- `testConfirmVerificationMarksAccountAsVerified`
- `testResetPasswordUpdatesHashAndCompletesRequest`

Problematic pattern:

- `AtomicReference<String>` captures password hashes from DAO calls.
- `AtomicReference<Instant>` captures verification timestamps.
- `AtomicReference<EmailActionStatus>` captures consumed request status.
- `doAnswer` / `thenAnswer` mutate those holders.

Why it is wrong:

- The assertions prove which DAO method arguments were passed, not the externally observable account-auth behavior.

Recommended replacement:

- Use stateful fake DAOs or exact stubs that expose account/request state after the service call.
- Assert the behavior: account result, encoded password matches raw password, email request status, verification timestamp, reset redirect, and recorded mail dispatch.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/ModerationServiceImplTest.java`

Affected tests:

- `resolveReportWithUserBan_banExpiryIsInTheFuture`
- `resolveReportWithUserBan_usesProvidedBanDuration`
- `resolveReportWithUserBan_banIsLinkedToSourceReport`
- `resolveReviewReport_contentDeleted_softDeletesCorrectReview`
- `appealReport_storesSuppliedAppealText`
- `softDeleteReview_forwardsAllArgumentsToDao`
- `restoreReview_forwardsUserIdsToDao`
- `softDeleteMatch_forwardsAllArgumentsToDao`

Problematic pattern:

- `AtomicLong` and `AtomicReference` capture DAO arguments.
- Several tests are explicitly named as pass-through/forwarding tests.

Why it is wrong:

- These tests assert implementation delegation rather than moderation behavior.
- The clearest conceptual violations are the `forwardsAllArgumentsToDao` tests.

Recommended replacement:

- For ban behavior, use a fake `UserBanDao` that stores the created `UserBan` and assert returned/moderation-visible state.
- For review and match deletion behavior, use exact stubs or small fake DAO state and assert service return values or resulting report behavior.
- Rename tests away from forwarding language.
- If a method is pure pass-through and only returns DAO success/failure, assert the public return value with exact expected setup rather than captured arguments.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/PlayerReviewServiceImplTest.java`

Affected test:

- `testDeleteReviewDelegatesToDao`

Problematic pattern:

- Local `ArrayList<Long>` captures reviewer/reviewed IDs from invocation arguments.

Why it is wrong:

- The test proves delegation arguments, not deletion behavior.

Recommended replacement:

- Stub the DAO for the expected reviewer/reviewed users and assert the service completes without exception.
- Keep `testDeleteReviewRejectsMissingReview` as the stronger negative behavior.
- Rename the success test to behavior language, such as `testDeleteReviewSucceedsForExistingReview`.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/MatchServiceImplTest.java`

Affected tests:

- `testUpdateMatchFromPrivateToPublicCancelsPendingInvitations`
- `testUpdateMatchFromRequestOnlyToPrivateCancelsPendingRequests`
- `testUpdateMatchFromRequestOnlyToOpenApprovesPendingRequestsWhenThereIsSpace`

Problematic pattern:

- `AtomicBoolean` flags record DAO and notification calls.
- `AtomicInteger` counts notification invocations.
- `doAnswer` mutates flags.

Why it is wrong:

- The test asserts the internal collaboration sequence instead of the observable outcome of a privacy-policy transition.

Recommended replacement:

- Extend the existing `RecordingMailDispatchService` to record:
  - invitation opened to public
  - pending request closed by privacy change
  - join request approved
- Use exact DAO stubs to drive the transition.
- Assert returned/updated match data and recorded mail outputs.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/MatchParticipationServiceImplTest.java`

Affected tests:

- `testInviteUserToSeriesInvitesEligibleDatesOnlyAndSendsOneMail`
- `testAcceptInviteExpandsSeriesInvitation`
- `testAcceptSeriesInviteUsesFutureSeriesWhenAnchorIsClosed`
- `testDeclineInviteExpandsSeriesInvitation`

Problematic pattern:

- `ArrayList<Long>` captures invited match IDs from invocation arguments.
- `AtomicInteger` records accepted/declined row counts.
- `thenAnswer` mutates holders.

Why it is wrong:

- The tests prove DAO invocation details and row-count plumbing rather than service behavior.

Recommended replacement:

- For series invitation, fail fast if `inviteUser` is called for ineligible IDs and assert the one observable mail output with the expected occurrence count.
- For accept/decline, use exact DAO stubs and assert boundary notification output through `RecordingMailDispatchService`.
- Add mail fake methods for invite accepted/declined if missing.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/TournamentServiceImplTest.java`

Affected tests:

- `searchPublicTournamentsPaginatesAndNormalizesInvalidSortToSoonest`
- `searchPublicTournamentsUsesDistanceSortOnlyWhenCoordinatesArePresent`

Problematic pattern:

- `AtomicReference<TournamentSort>` and `AtomicInteger` capture DAO sort, offset, and limit.

Why it is wrong:

- The test inspects the DAO call rather than the public search result.

Recommended replacement:

- Stub `findPublicTournaments` with exact normalized sort/page arguments.
- Assert returned page, total count, and item behavior.
- Keep pagination normalization coverage through the returned `PaginatedResult`.

Action:

- Rewrite; keep coverage.

### `services/src/test/java/ar/edu/itba/paw/services/TournamentBracketServiceImplTest.java`

Affected test/helper areas:

- `declareWinnerUpdatesEloForRatedSports`
- `configureMatchCreation`
- `configureStatefulBracket`

Problematic pattern:

- `AtomicReference<List<User>>` captures ELO service args.
- `AtomicLong` generates fake match IDs.

Why it is wrong:

- ELO capture tests the internal collaborator call.
- Fake ID generation is not conceptually wrong, but keeping `AtomicLong` leaves the correction visibly unaddressed.

Recommended replacement:

- Use a recording fake `UserSportRatingService` for ELO as an external boundary and assert recorded winner/loser behavior.
- Replace `AtomicLong` counters with deterministic IDs based on current fake collection size or a small mutable helper field in the fake.

Action:

- Rewrite ELO test; cleanup ID counters.

### `services/src/test/java/ar/edu/itba/paw/services/TournamentRegistrationServiceImplTest.java`

Affected helper:

- `configureCloseRegistrationWithTeamCreation`

Problematic pattern:

- `AtomicLong` generates fake team IDs.

Why it is wrong:

- Not an implementation-detail assertion, but it leaves `AtomicLong` in the test suite.

Recommended replacement:

- Replace `teamIds.getAndIncrement()` with `100L + createdTeams.size()` before adding the created team.

Action:

- Cleanup.

### `webapp/src/test/java/ar/edu/itba/paw/webapp/config/AuthenticatedLocalePersistenceInterceptorTest.java`

Affected tests:

- `authenticatedLangSwitchPersistsResolvedLanguage`
- `authenticatedPreservedLangParameterDoesNotPersistLanguage`
- `unsupportedLangSwitchDoesNotPersistDefaultLanguage`
- `anonymousLangSwitchDoesNotPersistLanguage`

Problematic pattern:

- `AtomicLong` and `AtomicReference<String>` capture `UserService.updatePreferredLanguage`.
- Negative cases use holder defaults to prove no call happened.

Why it is wrong:

- The tests assert whether a collaborator was invoked, not locale persistence behavior.

Recommended replacement:

- Replace mocked `UserService` with a small fake implementation that stores language-update records as test-double state.
- Assert update list size and contents.
- Keep HTTP status assertions.

Action:

- Rewrite; keep coverage.

### `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/FeedControllerTournamentTest.java`

Affected tests:

- `defaultFeedStillUsesMatchSearch`
- `tournamentFeedUsesTournamentSearchAndBuildsTournamentCards`

Problematic pattern:

- `AtomicBoolean` flags record whether match/tournament search was called.
- `AtomicReference<String>` captures tournament filters.

Why it is wrong:

- The tests prove selected service method calls, while the observable behavior is the feed mode and rendered cards.

Recommended replacement:

- Return distinct match and tournament data.
- Assert `FeedPageViewModel` contents, selected type, cards, badge, hrefs, and pagination.
- Use exact tournament stubbing for sport/sort if necessary, without holder mutation.

Action:

- Rewrite; keep coverage.

### `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/ModerationAdminControllerTest.java`

Affected test:

- `postBanUserUsesSubmittedDuration`

Problematic pattern:

- `AtomicInteger` captures `banDays`.

Why it is wrong:

- It proves an argument was passed to the service, not controller behavior.

Recommended replacement:

- Stub `moderationService.resolveReport` with exact `banDays = 30`.
- Assert redirect and flash behavior.

Action:

- Rewrite; keep coverage.

### `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/HostTournamentControllerTest.java`

Affected tests:

- `postCreateWithValidFormRedirectsToTournamentDetail`
- `postCreateWithoutTimezoneUsesArgentinaFallback`
- `postCreateWithTeamDraftOnlyRedirectsToTournamentDetail`
- `postCreateWithInvalidFormReturnsForm`
- `postCreateWithNoJoinModeReturnsForm`
- `postCreateWithUnsupportedTeamSizeForSportReturnsForm`
- `postEditTournamentWithValidFormRedirectsToTournamentDetail`
- `postCancelTournamentByHostRedirectsToTournamentDetail`
- `postPublishBracketWithValidRoundOneScheduleRedirectsToDetail`
- `postPublishBracketWithMissingScheduleRedirectsToSetup`

Problematic pattern:

- Class-level `AtomicReference` fields capture created/updated/cancel/publish requests.
- Invalid-form tests assert captured request remains null.

Why it is wrong:

- Success tests inspect service-call DTOs.
- Invalid-form tests use lack of service call as implementation-detail proof when model errors already prove the route behavior.

Recommended replacement:

- Use exact or `argThat` stubs for request content needed to return the test tournament.
- For invalid forms, assert HTTP status, view, and field errors only.
- For publish schedule, match expected schedule list through stub matching or assert redirect/model behavior.

Action:

- Rewrite; keep coverage.

### `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/UiRouteTest.java`

Affected areas:

- class fields from `lastSportsFilter` through `lastUpdatedMatch`
- `getFeedRouteWithRepeatedSportParamsPassesCommaSeparatedToService`
- reservation/cancellation route tests around match and series reservations
- series join-request route tests
- host publish form-to-request tests
- host edit and cancel tests
- event-list tests using `currentUserHas...` flags

Problematic pattern:

- Many `AtomicReference` fields store cross-test fake-service state.
- Some fields drive scenario setup; others prove specific service calls happened.

Why it is wrong:

- The route tests mix HTTP observable behavior with implementation-call proof.
- Cross-test `AtomicReference` state makes the fixture harder to reason about and visually matches the sprint correction.

Recommended replacement:

- Introduce named fake service classes with ordinary scenario fields, for example:
  - `FakeMatchService`
  - `FakeMatchReservationService`
  - `FakeMatchParticipationService`
- Keep state only where it models scenario data used by fake service behavior.
- Remove assertions that only prove a route selected an internal service method when redirect, flash, status, or model already prove the behavior.
- For form-to-request mapping tests, assert the returned fake match/model result or fake service state using plain fields, not `AtomicReference`.

Action:

- Rewrite; keep coverage.

## Implementation Iterations

Do not attempt the full plan as one bulk rewrite. The affected tests use different fixture styles, and `UiRouteTest` is large enough to deserve its own isolated pass. Each iteration should leave the test suite for that slice green before moving on.

## Progress Checklist

- [x] Service tests except tournament bracket:
  - [x] `AccountAuthServiceImplTest`
  - [x] `ModerationServiceImplTest`
  - [x] `PlayerReviewServiceImplTest`
  - [x] `MatchServiceImplTest`
  - [x] `MatchParticipationServiceImplTest`
  - Verification run: `mvn -pl services -Dtest="AccountAuthServiceImplTest,ModerationServiceImplTest,PlayerReviewServiceImplTest,MatchServiceImplTest,MatchParticipationServiceImplTest" test`
  - Result: 120 tests run, 0 failures/errors.
- [ ] Tournament service tests:
  - [ ] `TournamentServiceImplTest`
  - [ ] `TournamentRegistrationServiceImplTest`
  - [ ] `TournamentBracketServiceImplTest`
- [ ] Small webapp controller tests:
  - [ ] `AuthenticatedLocalePersistenceInterceptorTest`
  - [ ] `FeedControllerTournamentTest`
  - [ ] `ModerationAdminControllerTest`
  - [ ] `HostTournamentControllerTest`
- [ ] Large `UiRouteTest` fixture refactor.
- [ ] Final cleanup of imports and remaining holder-mutating `thenAnswer`/`doAnswer` blocks.
- [ ] Final scans:
  - [ ] `rg -n --glob '*/src/test/**' "AtomicReference|AtomicBoolean|AtomicInteger|AtomicLong" .`
  - [ ] `rg -n --glob '*/src/test/**' "Mockito\.verify|\bverify\(" .`
- [ ] Full test suite: `mvn test`

1. Service tests except tournament bracket:
   - `AccountAuthServiceImplTest`
   - `ModerationServiceImplTest`
   - `PlayerReviewServiceImplTest`
   - `MatchServiceImplTest`
   - `MatchParticipationServiceImplTest`
2. Tournament service tests:
   - `TournamentServiceImplTest`
   - `TournamentRegistrationServiceImplTest`
   - `TournamentBracketServiceImplTest`
3. Small webapp controller tests:
   - locale interceptor
   - feed tournament controller
   - moderation admin controller
   - host tournament controller
4. Large `UiRouteTest` fixture refactor only.
5. Final cleanup of imports and remaining `thenAnswer` blocks that only mutate holders.
6. Final scan and full test suite.

## Test Commands

Run after each group:

```bash
mvn -pl services -Dtest="AccountAuthServiceImplTest,ModerationServiceImplTest,PlayerReviewServiceImplTest" test
```

```bash
mvn -pl services -Dtest="MatchServiceImplTest,MatchParticipationServiceImplTest" test
```

```bash
mvn -pl services -Dtest="TournamentServiceImplTest,TournamentBracketServiceImplTest,TournamentRegistrationServiceImplTest" test
```

```bash
mvn -pl webapp -Dtest="AuthenticatedLocalePersistenceInterceptorTest,FeedControllerTournamentTest,ModerationAdminControllerTest,HostTournamentControllerTest" test
```

```bash
mvn -pl webapp -Dtest="UiRouteTest" test
```

Final verification:

```bash
rg -n --glob '*/src/test/**' "AtomicReference|AtomicBoolean|AtomicInteger|AtomicLong" .
```

```bash
rg -n --glob '*/src/test/**' "Mockito\.verify|\bverify\(" .
```

```bash
mvn test
```

## Acceptance Criteria

- No test imports or uses `AtomicReference`, `AtomicBoolean`, `AtomicInteger`, or `AtomicLong`.
- No test uses mutable arrays, custom holders, or local mutable collections only to prove mock invocation.
- No holder-mutating `doAnswer` or `thenAnswer` blocks remain.
- No new `Mockito.verify(...)`, `Mockito.never(...)`, or `ArgumentCaptor` usage is introduced.
- Tests assert behavior through outputs, state, model/flash responses, fake persistence state, or boundary outputs.
- Full `mvn test` passes.
