# Security And Permissions Cleanup Plan

This plan tracks the repository-wide cleanup requested after the sprint
correction:

> Tienen logica de seguridad/permisos implementada en los controllers, por
> ejemplo el metodo `isMatchVisibleToUser`. Esto corresponde a Spring Security y
> rompe la division de responsabilidades.

The goal is not to move every boolean into `SecurityConfig`. The intended
division is:

- `SecurityConfig` owns route-level authentication and role access.
- Services own resource-level authorization, ownership, visibility, lifecycle,
  and business permissions.
- Controllers bind input, call services, choose views or redirects, and translate
  service denial outcomes to HTTP status or flash messages.
- JSPs may hide or show controls using view state, but POST/service paths must
  still enforce the rule.

## Progress Tracking

Use this as the working checklist. Mark a module complete only after the code
change, targeted tests, and the module acceptance criteria are all done.

- [x] Module 1: Inventory controller permissions.
- [x] Module 2: Establish shared authorization vocabulary.
- [x] Module 3: Match detail visibility.
- [x] Module 4: Match detail action state.
- [x] Module 5: Tournament detail permissions.
- [x] Module 6: Tournament bracket permissions.
- [x] Module 7: Centralize admin/mod role checks.
- [ ] Module 8: Public profile, reviews, and report affordances.
- [ ] Module 9: Host controllers and mutating paths.
- [ ] Module 10: `SecurityConfig` route audit.
- [ ] Module 11: JSP and view model sweep.
- [ ] Module 12: Final verification.

Current review notes:

- The repository already has `SecurityService`; extend or repair it before
  adding another security helper.
- `TournamentBracketService.getBracket` already enforces bracket read access,
  but the controller still computes management/display capabilities.
- `PublicProfileController` owns both review eligibility display and
  report-user affordance logic.
- `MatchServiceImpl.cancelMatch` has a service-layer authorization bug: it
  compares `actingUser` to the current authenticated user instead of requiring
  host ownership or an approved elevated override.
- Fixed after Module 1: `ModerationService.appealReport` now receives the
  acting reporter and enforces report ownership in the service layer.

## Review Rules

Use these rules while reviewing each module:

- No controller should decide whether the current user owns a resource.
- No controller should decide whether `ADMIN_MOD` can override ownership.
- No controller should decide whether a private, draft, cancelled, or hidden
  resource is visible.
- No controller should implement business permission checks such as whether a
  user can join, reserve, cancel, edit, close, publish, or record a result.
- `SecurityContextHolder` should only appear in security infrastructure, not
  ordinary controllers or domain services.
- Allowed security-infrastructure hits include authentication providers,
  filters, typed-principal/current-user adapters, verification/login flows that
  establish authentication, and the approved security service/helper.
- Current-user extraction in controllers is allowed only to pass the actor to a
  service or to select presentation-only state that is not enforcing access.

## Module 1: Inventory Controller Permissions

Objective: create a complete checklist of controller-side security and
permission decisions before changing behavior.

Files to inspect first:

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/EventPageSupport.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/TournamentController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/PublicProfileController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/HostController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/HostTournamentController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/HostParticipationController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/ModerationReportController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/UserModerationReportController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/AccountController.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/MatchDashboardPageSupport.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/utils/EventCardViewModelUtils.java`

Search command:

```powershell
rg -n "isMatchVisibleToUser|isAdminMod|isHost|can[A-Z]|hostCan|reviewCanSubmit|reportUserCanSubmit|currentUser\\.getId|SecurityContextHolder|CurrentAuthenticatedUser|getAuthorities|UserRole\\.ADMIN_MOD|findReportById" webapp/src/main/java/ar/edu/itba/paw/webapp/controller webapp/src/main/java/ar/edu/itba/paw/webapp/utils
```

Acceptance criteria:

- [x] Every hit is classified as one of: route authentication, resource
  authorization, business permission, presentation-only state, or harmless view
  formatting.
- [x] Each resource authorization or business permission hit is assigned to a later
  module.
- [x] Presentation-only hits are documented with the service write path that
  still enforces the rule.
- [x] The inventory explicitly calls out any service-layer authorization bug
  discovered during the controller sweep.

### Module 1 Inventory - 2026-06-05

Route authentication and security infrastructure:

- `AuthController` uses `CurrentAuthenticatedUser` only to redirect already
  authenticated users away from login/register/password-reset pages. Classify as
  route/authentication UX; no later module.
- `SecurityControllerUtils` is the current web-layer adapter for requiring the
  current user, reading an optional current user, and refreshing the principal
  after profile edits. Classify as allowed security infrastructure; keep under
  Module 7 when role/current-user vocabulary is cleaned up.
- `VerificationController` creates a Spring Security context after successful
  account verification and maps roles to authorities. Classify as allowed
  authentication infrastructure; no domain-authorization module.
- `AccountController`, `HostController`, `HostTournamentController`,
  `HostParticipationController`, `ModerationAdminController`,
  `ModerationReportController`, `UserModerationReportController`, and
  `UserBanAppealController` use `SecurityControllerUtils.requireAuthenticatedUser`
  or `currentUserOrNull` to pass the actor to services or render the current
  account. These uses are acceptable unless called out below.

Resource authorization and business permissions:

- `EventPageSupport.isMatchVisibleToUser` and the recurring occurrence filter
  around `EventPageSupport:151` and `EventPageSupport:543` are match visibility
  authorization. Assign to Module 3.
- `EventPageSupport` computes match action state through `isHostViewer`,
  `isHost`, `canHostEdit`, `canHostCancel`, `canHostManageParticipants`,
  `canReserveMatch`, `canRequestToJoin`, `canCancelReservation`, and recurring
  reservation/join request evaluation. Assign to Module 4. Service write paths
  still include `MatchService.updateMatch`, `MatchService.cancelMatch`,
  `MatchReservationService`, and `MatchParticipationService`.
- `TournamentController.buildTournamentPage` computes `canJoinSolo`,
  `canLeaveSolo`, `requiresLoginToJoin`, `canCloseRegistration`,
  `canEditTournament`, `canCancelTournament`, `canManageBracket`,
  `canViewBracket`, `isHost`, and `isAdminMod`. Assign to Module 5, with direct
  role inspection also feeding Module 7. Write paths still go through
  `TournamentRegistrationService` and `TournamentService`.
- `TournamentController.showBracket` and `buildBracketPage` compute
  `matchDatesSetupPath`, `canManageResults`, per-match `canRecordResult`, and
  admin/mod override checks. Assign to Module 6, with role inspection also
  feeding Module 7. `TournamentBracketService.getBracket` already gates bracket
  reads, and bracket mutations still go through `TournamentBracketService`.
- `PublicProfileController` computes `reportUserCanSubmit`, `reviewCanSubmit`,
  self-profile review/report suppression, and locked-review messaging. Assign
  to Module 8. Write paths still go through `ModerationService.reportContent`
  and `PlayerReviewService.submitReview/deleteReview`.
- `UserModerationReportController.showMyReportDetail` filters
  `findReportById(reportId)` by reporter ownership and computes
  `appealAllowed`. Assign to Module 8 because report read state and appeal
  eligibility should be service-owned.
- `UserModerationReportController.appealReport` previously called the service
  without an acting user. Fixed after Module 1 by passing the reporter into
  `ModerationService.appealReport`; broader report read-state cleanup remains
  Module 8.
- `UserBanAppealController` derives ban appeal display state from the current
  user's active ban and the linked moderation report. Assign appeal eligibility
  read state to Module 8; the active-ban lookup itself is acceptable actor
  passing.
- `HostParticipationController` re-checks match join policy in controller
  methods for pending requests and invites, and computes `includeSeries`.
  Assign to Module 9. Ownership checks are mostly service-backed, but policy and
  recurrence eligibility should move behind `MatchParticipationService`.
- `HostTournamentController.showEditTournament` checks `isEditable` in the
  controller after `findTournamentForHost`, and `showBracketSetup` has a
  controller-owned 403-vs-404 fallback using `findPublicTournament`. Assign to
  Module 9.

Presentation-only or harmless formatting:

- `EventCardViewModelUtils.relationshipBadgesFor` and
  `tournamentRelationshipBadges` add relationship badges such as hosted,
  pending, invited, and going. Classify as presentation-only for now because the
  cards do not authorize writes; the related service write paths are
  `MatchReservationService` and `MatchParticipationService`. Revisit in Module
  11 to decide whether card relationship state should be service-produced.
- `MatchDashboardPageSupport` obtains the current user only to pass it into
  card building. Classify with the card badge sweep in Module 11.
- `ModerationReportController` uses `currentUser.getId()` in report submission
  logging and passes the actor to `ModerationService.reportContent`, which
  enforces self-report, duplicate-report, and report-limit rules. Classify as
  acceptable actor passing/logging; profile report affordance remains Module 8.
- `ModerationAdminController` show/hide flags such as `showResolution`,
  `showAppeal`, and latest-ban display are admin screen read state. Classify as
  presentation read state for now; leave any admin report read-model cleanup to
  Module 11 unless a later moderation task touches it.

Service-side bugs captured during the sweep:

- `MatchServiceImpl.cancelMatch` authorizes a non-host when `actingUser` equals
  the current authenticated user. Assign to Module 9 after Module 2 chooses the
  shared admin/mod override convention.
- Fixed after Module 1: `ModerationService.appealReport` now receives the
  acting reporter and rejects non-owned reports without revealing that the
  report exists. Broader report detail/read-state cleanup remains Module 8.

## Module 2: Establish Shared Authorization Vocabulary

Objective: avoid moving logic to random new helpers with inconsistent names.

Decisions to make before implementation:

- Whether to extend the existing `SecurityService` or create focused
  service-layer policy helpers such as `MatchAuthorizationService` and
  `TournamentAuthorizationService`.
- Whether service methods should keep receiving `User actingUser` plus
  centralized role helpers, or move to a small actor object that is built from
  the authenticated principal outside controllers.
- Which denial shape each service uses: existing domain exceptions, `Optional`
  for invisible/not found resources, or explicit result types.

Preferred minimal direction:

- Keep static URL rules in `SecurityConfig`.
- Extend/repair `SecurityService` before adding new security infrastructure.
- Add an elevated-role helper such as `canActAsAdminMod(User actingUser)` or
  `isCurrentUserAdminMod()`, and ensure it verifies the authenticated principal
  instead of trusting a detached `User`.
- Add service-layer methods for visible/readable resources.
- Add service-layer methods or result objects for page affordances.
- Do not make `services` depend on webapp security classes. If replacing the
  current reflection-based principal extraction, move a small principal contract
  to a lower module first or keep the extraction isolated inside the approved
  security helper.

Acceptance criteria:

- [x] One convention is chosen before refactoring match and tournament flows.
- [x] The convention does not require controllers to inspect roles or ownership.
- [x] Ordinary services no longer read `SecurityContextHolder` directly.
- [x] Tests can exercise regular-user and admin/mod paths without duplicating
  authority-inspection logic in each service test.

### Module 2 Convention - 2026-06-05

Chosen convention:

- Keep service methods receiving `User actingUser`; do not introduce a new actor
  object yet.
- Keep route-level authentication and role URL rules in `SecurityConfig`.
- Keep current-user extraction in controllers only for passing the actor to
  services or harmless presentation state.
- Use `SecurityService` as the single service-layer bridge to Spring Security.
- Use `SecurityService.canActAsAdminMod(User actingUser)` for elevated
  ownership overrides. The helper verifies that the authenticated principal
  matches `actingUser` and has the `ROLE_ADMIN_MOD` authority, so services do
  not trust detached user objects for elevated access.
- Keep visibility/read methods returning `Optional` for hidden/not-found reads
  unless a flow already uses a domain exception/result type.
- Keep mutating service methods using existing domain exceptions or result
  types, with controllers translating those outcomes to `404`, `403`, flash
  messages, or redirects.

Implementation notes:

- Added `SecurityService.canActAsAdminMod(User actingUser)` and tests for
  matching admin, matching regular user, and detached actor mismatch.
- Replaced direct `SecurityContextHolder`/authority reads in
  `TournamentServiceImpl`, `TournamentRegistrationServiceImpl`, and
  `TournamentBracketServiceImpl` with the new helper.
- Tournament service tests now mock the helper for admin/mod paths instead of
  constructing Spring Security authentication contexts.

## Module 3: Match Detail Visibility

Objective: remove `isMatchVisibleToUser` from `EventPageSupport`.

Current problem:

- `EventPageSupport` loads a match with `matchService.findMatchById`.
- It then checks draft/private/cancelled visibility in the controller support
  class.
- It calls reservation and invitation services to decide whether the viewer may
  see the match.

Target shape:

- Add a service-layer read method such as:

```java
Optional<Match> findVisibleMatchById(Long matchId, User viewer);
```

- Move these visibility rules into the service layer:
  - draft matches are visible only to the host or allowed elevated users
  - private matches are visible to host, confirmed participants, invitees, or
    allowed elevated users
  - cancelled matches are visible only to users with a legitimate relationship
    or allowed elevated users
  - public visible matches remain publicly readable
- `EventPageSupport` should call the new method and translate empty result to
  `404`.

Tests to add or update:

- [x] Public match is visible to anonymous viewer.
- [x] Draft match is visible to host.
- [x] Draft match is visible to admin/mod when elevated reads are supported.
- [x] Draft match is not visible to stranger.
- [x] Private match is visible to invitee.
- [x] Private match is visible to confirmed participant.
- [x] Private match is not visible to unrelated user.
- [x] Cancelled match is hidden from unrelated user.

Acceptance criteria:

- [x] `isMatchVisibleToUser` no longer exists in `webapp`.
- [x] Controller support does not call multiple services to decide match
  visibility.
- [x] Match visibility is tested in the service layer.

Implementation notes:

- Added `MatchService.findVisibleMatchById(Long matchId, User viewer)` for
  detail-page reads and `MatchService.canViewMatch(Match match, User viewer)`
  for already-loaded recurring occurrences.
- Moved draft/private/cancelled/public visibility decisions into
  `MatchServiceImpl`, including admin/mod override through
  `SecurityService.canActAsAdminMod(User actingUser)`.
- Updated `EventPageSupport` to translate an empty visible read to `404` and to
  ask `MatchService` for occurrence href visibility.

## Module 4: Match Detail Action State

Objective: move match page permission booleans out of `EventPageSupport`.

Current suspicious controller-side decisions:

- `canHostEdit`
- `canHostCancel`
- `canHostManageParticipants`
- `canReserveMatch`
- `canRequestToJoin`
- `canCancelReservation`
- host ownership checks for match management panels
- recurring-series reservation and join request availability state

Target shape:

- Keep service write methods as the final enforcement point.
- Add a service-layer read result for detail-page actions, for example
  `MatchInteractionState`, containing booleans already computed by services.
- Compute match management capabilities from status, time, host ownership, and
  approved admin/mod override. Do not leave admin/mod intervention as a hidden
  controller special case.
- `EventPageSupport` only passes that state to the JSP model.

Tests to add or update:

- [x] Host edit/cancel affordances follow event status and time rules.
- [x] Admin/mod edit/cancel affordances follow the same lifecycle limits while
  overriding ownership.
- [x] Reservation affordance follows visibility, join policy, capacity, event
  time, and user relationship rules.
- [x] Join request affordance follows approval-required policy.
- [x] Cancel reservation affordance follows event time and status rules.
- [x] Recurring-series affordances use the same service-computed state as
  single-match affordances.

Acceptance criteria:

- [x] Controllers no longer compute match action permissions.
- [x] Existing POST paths still reject forbidden actions even when a button is
  manually submitted.
- [x] Service tests cover host, unrelated user, and admin/mod paths for the
  moved management rules.

Partial implementation notes:

- Added `MatchManagementPermissions` and
  `MatchService.getMatchManagementPermissions(Match match, User viewer)`.
- Moved host detail-page management affordances from `EventPageSupport` to
  `MatchServiceImpl`: manage panel, edit, cancel, participant management, and
  recurring-series edit/cancel flags.
- Covered host, unrelated user, completed match, ended match, and recurring
  host paths in `MatchServiceImplTest`.
- Added admin/mod override for match edit/cancel and recurring edit/cancel
  management affordances while preserving the same lifecycle limits.
- Added service-authorized by-id DAO update/cancel methods so the service can
  enforce ownership/admin rules before persistence mutates a match.
- Non-host, non-admin users are denied by the service for edit/cancel before the
  DAO write path is reached.
- Added `MatchInteractionState` and
  `MatchService.getMatchInteractionState(Match, List<Match>, User)` for
  player-facing match detail actions.
- Moved reservation, reservation cancellation, join request, invitation, pending
  request, and recurring-series detail affordances from `EventPageSupport` into
  `MatchServiceImpl`.
- Kept reservation and participation POST paths enforced by
  `MatchReservationService` and `MatchParticipationService`; `EventPageSupport`
  now only maps service-computed state to existing JSP model attributes.
- Added service tests for public direct reservation, confirmed-player
  cancellation, approval-required join requests, pending join request display,
  recurring-series reservation state, and recurring-series pending join request
  state.

## Module 5: Tournament Detail Permissions

Objective: remove tournament permission logic from `TournamentController`.

Current suspicious controller-side decisions:

- `canJoinSolo`
- `canLeaveSolo`
- `requiresLoginToJoin`
- `canCloseRegistration`
- `canEditTournament`
- `canCancelTournament`
- `canManageBracket`
- `canViewBracket`
- `canDefineMatchDates`
- `isHost`
- `isAdminMod`

Target shape:

- Tournament services compute join/leave and management capabilities from
  registration windows, tournament status, capacity, existing team/solo state,
  host ownership, and elevated role.
- `TournamentController.buildTournamentPage` receives or asks for a
  service-produced permissions/read model.
- Controller no longer checks host ownership or admin role.

Tests to add or update:

- [ ] Authenticated user can join solo only when registration, capacity, and
  existing registration state allow it.
- [ ] User can leave solo only while eligible.
- [ ] Host can manage tournament in allowed statuses.
- [ ] Unrelated user cannot manage tournament.
- [ ] Admin/mod can manage across ownership boundaries.
- [ ] Completed/cancelled lifecycle states block the correct actions.
- [ ] Anonymous users only get public read state and login-required affordances.

Acceptance criteria:

- [x] `TournamentController` no longer defines `isHost` or `isAdminMod`.
- [x] Tournament capability booleans are computed outside the controller.
- [x] `buildTournamentPage` no longer contains business-permission TODO logic.

Implemented in Module 5:

- Added `TournamentManagementPermissions` and `TournamentRegistrationState`
  service read models.
- Moved tournament detail join/leave, login-required, close-registration,
  edit/cancel, bracket visibility, bracket management, match-date setup, and
  result-management capability calculations out of `TournamentController`.
- Kept `TournamentController` responsible for labels, paths, flash messages,
  and JSP view-model assembly.
- Added service tests for solo join/leave/readiness state, anonymous
  affordances, host management, unrelated-user denial, admin/mod override, and
  completed/cancelled lifecycle blocking.

## Module 6: Tournament Bracket Permissions

Objective: remove bracket/result management checks from controller code.

Current suspicious controller-side decisions:

- `canManageResults`
- `canRecordResult`
- `canDefineMatchDates`
- `matchDatesSetupPath` selection based on controller-owned authorization

Target shape:

- Keep `TournamentBracketService.getBracket` as the read gate for bracket
  visibility.
- Extend bracket read data with viewer capabilities, or expose a dedicated
  permission/read-state method.
- Controller builds display rows, but does not decide whether the viewer may
  record results or manage match dates.

Tests to add or update:

- [ ] Host can access setup controls in `BRACKET_SETUP`.
- [ ] Admin/mod can access setup controls in `BRACKET_SETUP`.
- [ ] Unrelated user cannot access setup controls before public bracket states.
- [ ] Host can record eligible result.
- [ ] Admin/mod can record eligible result.
- [ ] Unrelated user cannot record result.
- [ ] Result cannot be recorded for incomplete or already decided matches.

Acceptance criteria:

- [x] Bracket POST service method remains the final enforcement point.
- [x] Bracket JSP receives display state without controller-owned authorization.
- [x] Controller no longer calls `canDefineMatchDates`, `canRecordResult`, or
  `isAdminMod`.

Implemented in Module 6:

- Extended `TournamentBracketView` with service-computed per-match
  result-recording display state.
- Moved result-recording row eligibility out of `TournamentController`; the
  controller now maps `TournamentBracketView.isResultRecordable(matchId)` into
  the JSP-facing bracket match view model.
- Kept `TournamentBracketService.declareWinner` as the final enforcement point
  for result mutation, including host/admin ownership, lifecycle state,
  incomplete matches, and already-decided matches.
- Added bracket service tests for host/admin setup read access, unrelated-user
  pre-public denial, host/admin/unrelated result affordances, incomplete
  matches, and already-decided matches.
- Added controller coverage for admin/mod setup controls and service-provided
  match result affordance state.

## Module 7: Centralize Admin/Mod Role Checks

Objective: remove duplicated direct Spring Security role checks outside security
infrastructure.

Current suspicious files:

- `services/src/main/java/ar/edu/itba/paw/services/MatchServiceImpl.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/TournamentController.java`

Fixed after Module 2:

- `TournamentServiceImpl`, `TournamentRegistrationServiceImpl`, and
  `TournamentBracketServiceImpl` no longer read `SecurityContextHolder`
  directly for admin/mod ownership overrides; they use
  `SecurityService.canActAsAdminMod(User actingUser)`.

Search command:

```powershell
rg -n "SecurityContextHolder|getAuthorities|ROLE_ADMIN_MOD|UserRole\\.ADMIN_MOD|isAdminMod|currentUser\\(\\)" webapp/src/main/java services/src/main/java
```

Target shape:

- Role checks exist only in security infrastructure or one approved security
  service/helper.
- Ordinary services call the approved helper or receive a role-aware actor.
- Controllers do not inspect roles.
- `SecurityServiceImpl` may read the security context; tournament/match domain
  services should not.
- `VerificationController`, `AccountAuthenticationProvider`,
  `CurrentAuthenticatedUser`, `SecurityControllerUtils`, and security filters
  are allowed web-layer hits when they are establishing or adapting
  authentication rather than authorizing domain resources.

Acceptance criteria:

- [x] No ordinary controller or service directly reads authorities.
- [x] Direct `SecurityContextHolder` usage outside approved infrastructure is
  removed or explicitly documented as authentication infrastructure.
- [x] Tests cover admin/mod ownership override paths.
- [x] Tests cover non-admin users failing the same ownership override paths.

Implemented in Module 7:

- Added `SecurityAuthorities` as the shared web-layer role-to-authority mapper.
- Reused the shared mapper from `AccountAuthenticationProvider` and the account
  verification authentication flow.
- Confirmed remaining direct security-context or authority hits are limited to
  approved authentication/security infrastructure: `SecurityServiceImpl`,
  `CurrentAuthenticatedUser`, `SecurityControllerUtils`,
  `VerificationController`, and `AccountAuthenticationProvider`. The
  `AdminBootstrapServiceImpl` hit assigns the configured bootstrap admin role
  and is not an authorization check.
- Existing service tests cover admin/mod override and non-admin denial paths for
  tournament/match ownership rules; verification-flow coverage now asserts the
  shared admin/mod authority mapping.

## Module 8: Public Profile, Reviews, And Report Affordances

Objective: remove profile/review permission decisions from
`PublicProfileController`.

Current suspicious logic:

- self-review checks in the controller
- review eligibility checks used for display
- review delete authorization should be enforced by `PlayerReviewService`
- `reportUserCanSubmit` self-report display logic in the controller
- locked-review message selection based on controller-owned eligibility state

Target shape:

- `PlayerReviewService` owns review eligibility, read state, and delete
  authorization.
- `ModerationService` owns report eligibility/read state for profile affordances,
  while `reportContent` remains the write enforcement point.
- Controller asks for service result/read state and renders it.
- Controller catches domain denial and redirects or returns the correct status.

Tests to add or update:

- [ ] User cannot review self.
- [ ] User can review only eligible players.
- [ ] User cannot delete someone else's review.
- [ ] Authorized delete still works.
- [ ] User cannot report self.
- [ ] Profile report affordance is hidden for anonymous and self-viewer cases.

Acceptance criteria:

- [ ] Review permission decisions live in service code.
- [ ] Report affordance decisions live in service code.
- [ ] Controller only translates service outcomes and chooses localized
  presentation strings.

## Module 9: Host Controllers And Mutating Paths

Objective: verify host controllers rely on services for ownership and lifecycle
checks.

Files to inspect:

- `HostController`
- `HostTournamentController`
- `HostParticipationController`
- `MatchServiceImpl`
- `TournamentServiceImpl`
- `TournamentRegistrationServiceImpl`
- `TournamentBracketServiceImpl`

Known service-side issue to fix:

- `MatchServiceImpl.cancelMatch` should not authorize a non-host merely because
  `actingUser` matches the authenticated user. It should use the same
  host-or-approved-admin/mod convention chosen in Module 2.

Expected pattern:

- Controller obtains `actingUser`.
- Controller calls service method with `actingUser`.
- Service checks ownership, admin/mod override where applicable, status, time,
  and resource existence.
- Controller maps service exception/result to redirect, flash, `404`, or `403`.

Acceptance criteria:

- [ ] No host controller manually checks whether the user owns the match or
  tournament.
- [ ] Match update, match cancellation, tournament update/cancellation,
  registration closing, bracket setup, and result recording enforce ownership
  or approved admin/mod override in services.
- [ ] Existing service methods have tests for denied ownership.
- [ ] Service tests cover admin/mod override where product rules allow elevated
  intervention.

## Module 10: SecurityConfig Route Audit

Objective: keep route-level security in Spring Security and avoid duplicating it
in controllers.

Review:

- public GET routes
- authenticated mutating match/tournament/review/report routes
- authenticated `/reports/mine/**` routes
- `/host/**`
- `/admin/**`
- `/moderation/**`
- banned-account behavior
- login/register/password-reset/verification routes

Acceptance criteria:

- [ ] Static route authentication and role rules are expressed in
  `SecurityConfig`.
- [ ] Per-resource authorization is not attempted through URL pattern hacks.
- [ ] Banned-account restrictions remain centralized in
  `BannedAccountAuthorizationFilter`.
- [ ] Controller tests or route tests cover at least one authenticated and denied
  path for changed access rules.

## Module 11: JSP And View Model Sweep

Objective: ensure JSPs only render service-provided state.

Search command:

```powershell
rg -n "can[A-Z]|hostCan|isHost|admin|role|permission|visible|reportUserCanSubmit|reviewCanSubmit" webapp/src/main/webapp/WEB-INF/views webapp/src/main/webapp/WEB-INF/tags webapp/src/main/java/ar/edu/itba/paw/webapp/viewmodel webapp/src/main/java/ar/edu/itba/paw/webapp/utils
```

Acceptance criteria:

- [ ] JSPs do not infer authorization from raw domain ownership.
- [ ] JSP affordances are backed by service-enforced POST paths.
- [ ] Event card relationship badges are classified as presentation-only or
  moved behind service-provided read state.
- [ ] New user-facing copy, if any, is present in both message bundles.

## Module 12: Final Verification

Run after the cleanup:

```powershell
rg -n "isMatchVisibleToUser|isAdminMod|SecurityContextHolder|CurrentAuthenticatedUser|getAuthorities|ROLE_ADMIN_MOD|UserRole\\.ADMIN_MOD|currentUser\\.getId\\(\\).*equals|equals\\(.*currentUser\\.getId" webapp/src/main/java services/src/main/java
mvn test
```

Expected remaining hits:

- Security infrastructure classes.
- Current-user extraction used only to pass an actor to services.
- Presentation-only state that is not enforcing access.
- Message/view-model fields named `can...` when their values come from service
  read state.

Final acceptance criteria:

- [ ] No controller owns resource authorization or business permission
  decisions.
- [ ] Route-level rules remain centralized in `SecurityConfig`.
- [ ] Service-layer tests cover allowed and denied paths for each moved rule.
- [ ] `mvn test` passes.

## Suggested Execution Order

Use this order when working through the cleanup together:

1. Module 1: inventory.
2. Module 2: shared authorization vocabulary.
3. Module 3: match visibility, because it is the correction's explicit example.
4. Module 4: match action state.
5. Module 7: admin/mod role centralization.
6. Module 5: tournament detail permissions.
7. Module 6: tournament bracket permissions.
8. Module 8: profile/reviews/report affordances.
9. Module 9: host controllers.
10. Module 10: route audit.
11. Module 11: JSP/view model sweep.
12. Module 12: final verification.
