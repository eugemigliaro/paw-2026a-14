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
- Current-user extraction in controllers is allowed only to pass the actor to a
  service or to build presentation state returned by a service.

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

Search command:

```powershell
rg -n "isMatchVisibleToUser|isAdminMod|isHost|can[A-Z]|hostCan|currentUser\\.getId|SecurityContextHolder|CurrentAuthenticatedUser|getAuthorities|UserRole\\.ADMIN_MOD" webapp/src/main/java/ar/edu/itba/paw/webapp/controller
```

Acceptance criteria:

- Every hit is classified as one of: route authentication, resource
  authorization, business permission, presentation-only state, or harmless view
  formatting.
- Each resource authorization or business permission hit is assigned to a later
  module.

## Module 2: Establish Shared Authorization Vocabulary

Objective: avoid moving logic to random new helpers with inconsistent names.

Decisions to make before implementation:

- Whether to extend `SecurityService` or create focused service-layer policy
  helpers such as `MatchAuthorizationService` and `TournamentAuthorizationService`.
- Whether service methods should receive `User actingUser` plus role-aware
  helpers, or a small actor object containing user and role.
- Which denial shape each service uses: existing domain exceptions, `Optional`
  for invisible/not found resources, or explicit result types.

Preferred minimal direction:

- Keep static URL rules in `SecurityConfig`.
- Add service-layer methods for visible/readable resources.
- Add service-layer methods or result objects for page affordances.
- Add `SecurityService.isAdminMod()` only if role-aware checks remain inside
  services.

Acceptance criteria:

- One convention is chosen before refactoring match and tournament flows.
- The convention does not require controllers to inspect roles or ownership.

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
  - private matches are visible to host, confirmed participants, or invitees
  - cancelled matches are visible only to users with a legitimate relationship
  - public visible matches remain publicly readable
- `EventPageSupport` should call the new method and translate empty result to
  `404`.

Tests to add or update:

- public match is visible to anonymous viewer
- draft match is visible to host
- draft match is not visible to stranger
- private match is visible to invitee
- private match is visible to confirmed participant
- private match is not visible to unrelated user
- cancelled match is hidden from unrelated user

Acceptance criteria:

- `isMatchVisibleToUser` no longer exists in `webapp`.
- Controller support does not call multiple services to decide match visibility.
- Match visibility is tested in the service layer.

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

Target shape:

- Keep service write methods as the final enforcement point.
- Add a service-layer read result for detail-page actions, for example
  `MatchInteractionState`, containing booleans already computed by services.
- `EventPageSupport` only passes that state to the JSP model.

Tests to add or update:

- host edit/cancel affordances follow event status and time rules
- reservation affordance follows visibility, join policy, capacity, event time,
  and user relationship rules
- join request affordance follows approval-required policy
- cancel reservation affordance follows event time and status rules

Acceptance criteria:

- Controllers no longer compute match action permissions.
- Existing POST paths still reject forbidden actions even when a button is
  manually submitted.

## Module 5: Tournament Detail Permissions

Objective: remove tournament permission logic from `TournamentController`.

Current suspicious controller-side decisions:

- `canCloseRegistration`
- `canEditTournament`
- `canCancelTournament`
- `canManageBracket`
- `canViewBracket`
- `canDefineMatchDates`
- `isHost`
- `isAdminMod`

Target shape:

- Tournament services compute management capabilities from tournament status,
  host ownership, and elevated role.
- `TournamentController.buildTournamentPage` receives or asks for a
  service-produced permissions/read model.
- Controller no longer checks host ownership or admin role.

Tests to add or update:

- host can manage tournament in allowed statuses
- unrelated user cannot manage tournament
- admin/mod can manage across ownership boundaries
- completed/cancelled lifecycle states block the correct actions
- anonymous users only get public read state

Acceptance criteria:

- `TournamentController` no longer defines `isHost` or `isAdminMod`.
- Tournament capability booleans are computed outside the controller.

## Module 6: Tournament Bracket Permissions

Objective: remove bracket/result management checks from controller code.

Current suspicious controller-side decisions:

- `canManageResults`
- `canRecordResult`
- `canDefineMatchDates`

Target shape:

- `TournamentBracketService` returns bracket view data with viewer capabilities,
  or exposes a dedicated permission/read-state method.
- Controller builds display rows, but does not decide whether the viewer may
  record results or manage match dates.

Tests to add or update:

- host can record eligible result
- admin/mod can record eligible result
- unrelated user cannot record result
- result cannot be recorded for incomplete or already decided matches

Acceptance criteria:

- Bracket POST service method remains the final enforcement point.
- Bracket JSP receives display state without controller-owned authorization.

## Module 7: Centralize Admin/Mod Role Checks

Objective: remove duplicated direct Spring Security role checks outside security
infrastructure.

Current suspicious files:

- `services/src/main/java/ar/edu/itba/paw/services/TournamentServiceImpl.java`
- `services/src/main/java/ar/edu/itba/paw/services/TournamentRegistrationServiceImpl.java`
- `services/src/main/java/ar/edu/itba/paw/services/TournamentBracketServiceImpl.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/TournamentController.java`

Search command:

```powershell
rg -n "SecurityContextHolder|getAuthorities|ROLE_ADMIN_MOD|UserRole\\.ADMIN_MOD|isAdminMod" webapp/src/main/java services/src/main/java
```

Target shape:

- Role checks exist only in security infrastructure or one approved security
  service/helper.
- Ordinary services call the approved helper or receive a role-aware actor.
- Controllers do not inspect roles.

Acceptance criteria:

- No ordinary controller or service directly reads authorities.
- Tests cover admin/mod ownership override paths.

## Module 8: Public Profile And Reviews

Objective: remove profile/review permission decisions from
`PublicProfileController`.

Current suspicious logic:

- self-review checks in the controller
- review eligibility checks used for display
- review delete authorization should be enforced by `PlayerReviewService`

Target shape:

- `PlayerReviewService` owns review eligibility and delete authorization.
- Controller asks for service result/read state and renders it.
- Controller catches domain denial and redirects or returns the correct status.

Tests to add or update:

- user cannot review self
- user can review only eligible players
- user cannot delete someone else's review
- authorized delete still works

Acceptance criteria:

- Review permission decisions live in service code.
- Controller only translates service outcomes.

## Module 9: Host Controllers And Mutating Paths

Objective: verify host controllers rely on services for ownership and lifecycle
checks.

Files to inspect:

- `HostController`
- `HostTournamentController`
- `HostParticipationController`

Expected pattern:

- Controller obtains `actingUser`.
- Controller calls service method with `actingUser`.
- Service checks ownership, admin/mod override where applicable, status, time,
  and resource existence.
- Controller maps service exception/result to redirect, flash, `404`, or `403`.

Acceptance criteria:

- No host controller manually checks whether the user owns the match or
  tournament.
- Existing service methods have tests for denied ownership.

## Module 10: SecurityConfig Route Audit

Objective: keep route-level security in Spring Security and avoid duplicating it
in controllers.

Review:

- public GET routes
- authenticated mutating match/tournament/review/report routes
- `/host/**`
- `/admin/**`
- banned-account behavior
- login/register/password-reset/verification routes

Acceptance criteria:

- Static route authentication and role rules are expressed in `SecurityConfig`.
- Per-resource authorization is not attempted through URL pattern hacks.
- Controller tests or route tests cover at least one authenticated and denied
  path for changed access rules.

## Module 11: JSP And View Model Sweep

Objective: ensure JSPs only render service-provided state.

Search command:

```powershell
rg -n "can[A-Z]|hostCan|isHost|admin|role|permission|visible" webapp/src/main/webapp/WEB-INF/views webapp/src/main/webapp/WEB-INF/tags webapp/src/main/java/ar/edu/itba/paw/webapp/viewmodel
```

Acceptance criteria:

- JSPs do not infer authorization from raw domain ownership.
- JSP affordances are backed by service-enforced POST paths.
- New user-facing copy, if any, is present in both message bundles.

## Module 12: Final Verification

Run after the cleanup:

```powershell
rg -n "isMatchVisibleToUser|isAdminMod|SecurityContextHolder|CurrentAuthenticatedUser|getAuthorities|ROLE_ADMIN_MOD|currentUser\\.getId\\(\\).*equals|equals\\(.*currentUser\\.getId" webapp/src/main/java services/src/main/java
mvn test
```

Expected remaining hits:

- Security infrastructure classes.
- Current-user extraction used only to pass an actor to services.
- Presentation-only state that is not enforcing access.

Final acceptance criteria:

- No controller owns resource authorization or business permission decisions.
- Route-level rules remain centralized in `SecurityConfig`.
- Service-layer tests cover allowed and denied paths for each moved rule.
- `mvn test` passes.

## Suggested Execution Order

Use this order when working through the cleanup together:

1. Module 1: inventory.
2. Module 2: shared authorization vocabulary.
3. Module 3: match visibility, because it is the correction's explicit example.
4. Module 4: match action state.
5. Module 7: admin/mod role centralization.
6. Module 5: tournament detail permissions.
7. Module 6: tournament bracket permissions.
8. Module 8: profile/reviews.
9. Module 9: host controllers.
10. Module 10: route audit.
11. Module 11: JSP/view model sweep.
12. Module 12: final verification.
