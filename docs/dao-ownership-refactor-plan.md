# DAO Ownership Refactor Plan

## Summary

Address the sprint correction by making DAO injection single-owner across the service
layer. The plan uses internal service-module ports/facades, not public
`service-contracts`, so web-facing APIs stay stable.

Success criteria:

- In `services/src/main/java`, each DAO type is injected by exactly one owning service
  or internal owner port.
- Cross-aggregate service code calls services/internal ports, not DAOs directly.
- External controller-facing service contracts remain unchanged unless a test reveals a
  necessary behavior gap.
- `mvn test` passes.

## Progress Checklist

Use this checklist to track the refactor one small slice at a time. Mark a slice
complete only after production code, affected tests, formatting, and at least the
relevant focused Maven test pass.

1. [x] Add `UserDataService` / `UserDataServiceImpl`.
2. [x] Move `UserDao` access in `UserServiceImpl`, `AccountAuthServiceImpl`,
   `AdminBootstrapServiceImpl`, and `ModerationServiceImpl` behind
   `UserDataService`.
3. [x] Update affected `UserDao`-based service tests to mock/use
   `UserDataService`.
4. [x] Verify `UserDao` ownership with:

   ```bash
   rg "private final .*UserDao|UserDao" services/src/main/java/ar/edu/itba/paw/services
   ```

   Expected current result: only `UserDataServiceImpl`.

5. [x] Add `PlayerReviewDataService` / `PlayerReviewDataServiceImpl`.
6. [x] Move `PlayerReviewDao` access in `PlayerReviewServiceImpl` and
   `ModerationServiceImpl` behind `PlayerReviewDataService`.
7. [x] Update affected `PlayerReviewDao`-based service tests.
8. [x] Add `TournamentDataService` / `TournamentDataServiceImpl`.
9. [x] Move `TournamentDao` access in `TournamentServiceImpl`,
   `TournamentRegistrationServiceImpl`, and `TournamentBracketServiceImpl` behind
   `TournamentDataService`.
10. [x] Update affected `TournamentDao`-based service tests.
11. [x] Add `TournamentTeamDataService` / `TournamentTeamDataServiceImpl`.
12. [x] Move `TournamentTeamDao` access in `TournamentRegistrationServiceImpl`,
    `TournamentBracketServiceImpl`, and `TournamentMailServiceImpl` behind
    `TournamentTeamDataService`.
13. [x] Update affected `TournamentTeamDao`-based service tests.
14. [ ] Add `MatchDataService` / `MatchDataServiceImpl`.
    - [x] Start `MatchDataService` with the `findById` operation needed by
      `SecurityServiceImpl`.
    - [ ] Expand `MatchDataService` to cover the remaining `MatchDao` operations
      as the larger match services move behind it.
15. [ ] Move `MatchDao` access in `MatchServiceImpl`,
    `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`,
    `ModerationServiceImpl`, `RecurringMatchAsyncService`, and
    `SecurityServiceImpl` behind `MatchDataService`.
    - [x] Move `SecurityServiceImpl` behind `MatchDataService`.
    - [ ] Move `MatchServiceImpl` behind `MatchDataService`.
    - [x] Move `MatchReservationServiceImpl` behind `MatchDataService`.
    - [ ] Move `MatchParticipationServiceImpl` behind `MatchDataService`.
    - [x] Move `ModerationServiceImpl` match access behind `MatchDataService`.
    - [x] Move `RecurringMatchAsyncService` behind `MatchDataService`.
16. [ ] Update affected `MatchDao`-based service tests.
    - [x] Update `SecurityServiceImplTest`.
    - [x] Update `ModerationServiceImplTest`.
    - [x] Update recurring occurrence setup in `MatchServiceImplTest`.
    - [x] Update `MatchReservationServiceImplTest`.
17. [ ] Add `MatchParticipantDataService` /
    `MatchParticipantDataServiceImpl`.
18. [ ] Move `MatchParticipantDao` access in `MatchServiceImpl`,
    `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`,
    `MatchNotificationServiceImpl`, and `ModerationServiceImpl` behind
    `MatchParticipantDataService`.
19. [ ] Update affected `MatchParticipantDao`-based service tests.
20. [ ] Run the final DAO ownership acceptance check:

    ```bash
    rg "private final .*Dao" services/src/main/java
    ```

    Expected result: only the final owner classes listed in the DAO ownership map.

21. [ ] Run `mvn spotless:apply`.
22. [ ] Run `mvn test`.

## DAO Ownership Map

| DAO | Final Owner | Current Extra Consumers To Remove | Replacement Route |
| --- | --- | --- | --- |
| `EmailActionRequestDao` | `AccountAuthServiceImpl` | none | no structural change |
| `ImageDao` | `ImageServiceImpl` | none | no structural change |
| `ModerationReportDao` | `ModerationServiceImpl` | none | no structural change |
| `TournamentMatchDao` | `TournamentBracketServiceImpl` | none | no structural change |
| `TournamentSoloEntryDao` | `TournamentRegistrationServiceImpl` | none | no structural change |
| `UserBanDao` | `ModerationServiceImpl` | none | no structural change |
| `UserSportRatingDao` | `UserSportRatingServiceImpl` | none | no structural change |
| `UserDao` | new internal `UserDataServiceImpl` | `UserServiceImpl`, `AccountAuthServiceImpl`, `AdminBootstrapServiceImpl`, `ModerationServiceImpl` | inject `UserDataService` |
| `MatchDao` | new internal `MatchDataServiceImpl` | `MatchServiceImpl`, `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`, `ModerationServiceImpl`, `RecurringMatchAsyncService`, `SecurityServiceImpl` | inject `MatchDataService` |
| `MatchParticipantDao` | new internal `MatchParticipantDataServiceImpl` | `MatchServiceImpl`, `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`, `MatchNotificationServiceImpl`, `ModerationServiceImpl` | inject `MatchParticipantDataService` |
| `PlayerReviewDao` | new internal `PlayerReviewDataServiceImpl` | `PlayerReviewServiceImpl`, `ModerationServiceImpl` | inject `PlayerReviewDataService` |
| `TournamentDao` | new internal `TournamentDataServiceImpl` | `TournamentServiceImpl`, `TournamentRegistrationServiceImpl`, `TournamentBracketServiceImpl` | inject `TournamentDataService` |
| `TournamentTeamDao` | new internal `TournamentTeamDataServiceImpl` | `TournamentRegistrationServiceImpl`, `TournamentBracketServiceImpl`, `TournamentMailServiceImpl` | inject `TournamentTeamDataService` |

Internal ports live under
`services/src/main/java/ar/edu/itba/paw/services/internal`. They are Spring beans,
but not exported through `service-contracts`.

## Implementation Changes

### Phase 1: Add Internal Owner Ports

Create these internal interfaces and implementations:

- `UserDataService` / `UserDataServiceImpl`
  - Wraps all current `UserDao` operations used by services: user/account creation,
    user/account lookup, profile updates, password hash update, email verification,
    and preferred language.
  - Replace direct `UserDao` injection in `UserServiceImpl`, `AccountAuthServiceImpl`,
    `AdminBootstrapServiceImpl`, and `ModerationServiceImpl`.
- `PlayerReviewDataService` / `PlayerReviewDataServiceImpl`
  - Wraps review submit/delete/restore/find/summary/count/list/can-review operations.
  - Replace direct `PlayerReviewDao` injection in `PlayerReviewServiceImpl` and
    `ModerationServiceImpl`.
- `TournamentDataService` / `TournamentDataServiceImpl`
  - Wraps tournament create/find/search/count/update/refresh operations.
  - Replace direct `TournamentDao` injection in `TournamentServiceImpl`,
    `TournamentRegistrationServiceImpl`, and `TournamentBracketServiceImpl`.
- `TournamentTeamDataService` / `TournamentTeamDataServiceImpl`
  - Wraps team create/member/list/seed/find/count operations.
  - Replace direct `TournamentTeamDao` injection in `TournamentRegistrationServiceImpl`,
    `TournamentBracketServiceImpl`, and `TournamentMailServiceImpl`.

### Phase 2: Refactor Match DAOs

Create these internal interfaces and implementations:

- `MatchDataService` / `MatchDataServiceImpl`
  - Wraps match create, series create, update, cancel, soft delete, restore, find by
    id, find public, series occurrence queries, public search/count, and dashboard
    search/count.
  - Replace direct `MatchDao` injection in `MatchServiceImpl`,
    `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`,
    `ModerationServiceImpl`, `RecurringMatchAsyncService`, and `SecurityServiceImpl`.
- `MatchParticipantDataService` / `MatchParticipantDataServiceImpl`
  - Wraps reservation, join request, invite, confirmed/pending participant,
    pending/invited match-id, and cancellation operations from `MatchParticipantDao`.
  - Replace direct `MatchParticipantDao` injection in `MatchServiceImpl`,
    `MatchReservationServiceImpl`, `MatchParticipationServiceImpl`,
    `MatchNotificationServiceImpl`, and `ModerationServiceImpl`.

Keep existing business service boundaries. Do not merge match services.

### Phase 3: Clean Up Constructors And Tests

- Update service constructors and fields so service implementations depend on internal
  owner ports instead of DAOs.
- Update unit tests to mock internal ports for refactored services.
- Keep persistence tests unchanged except for imports only if required.
- Add an architecture regression test or documented acceptance check that
  `rg "private final .*Dao" services/src/main/java` only returns the final owner
  classes listed above.

## Test Plan

Run:

```bash
mvn test
```

Update service tests for:

- `AccountAuthServiceImplTest`
- `AdminBootstrapServiceImplTest`
- `UserServiceImplTest`
- `ModerationServiceImplTest`
- `PlayerReviewServiceImplTest`
- `MatchServiceImplTest`
- `MatchReservationServiceImplTest`
- `MatchParticipationServiceImplTest`
- `MatchNotificationServiceImplTest`
- `SecurityServiceImplTest`
- `TournamentServiceImplTest`
- `TournamentRegistrationServiceImplTest`
- `TournamentBracketServiceImplTest`
- `TournamentMailServiceImplTest`

Acceptance scenarios:

- Registration, verification, password reset, and admin bootstrap still use the same
  account behavior.
- Match create/update/cancel/search/reservation/join/invite flows keep current
  outcomes.
- Moderation can still resolve target names, delete/restore matches, delete/restore
  reviews, ban/unban users, and remove participants.
- Tournament create/search/register/close/bracket/mail flows keep current outcomes.
- DAO injection ownership check confirms no DAO is injected by multiple service-layer
  classes.

## Assumptions

- Addressing the correction means strict single-owner DAO injection, not merely
  documenting shared DAO usage.
- Internal service-module ports are acceptable and should not be added to
  `service-contracts`.
- This is a refactor plan only: no product behavior, routes, JSPs, migrations, or
  public APIs should change.
