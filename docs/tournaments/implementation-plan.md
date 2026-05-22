# Tournaments Implementation Plan

This plan turns the tournament feature brief into an incremental backlog for the
current Maven multi-module Spring MVC/JSP application.

The plan is intentionally staged. Do not start with the whole data model plus
every UI flow. Build a vertical spine first, then expand it.

## Ground Rules

- Use Java 21, Spring MVC, JSP/JSTL/EL, Spring DI, JPA/Hibernate, Flyway,
  PostgreSQL runtime, HSQLDB persistence tests, JUnit 5, Mockito, and MockMvc.
- Keep the layered architecture:
  - `models`: JPA entities and enums
  - `persistence-contracts`: DAO interfaces
  - `persistence`: JPA DAO implementations and Flyway migrations
  - `service-contracts`: service interfaces, requests, results, exceptions
  - `services`: business rules and orchestration
  - `webapp`: controllers, forms, view models, JSPs, tags, CSS, JS
- Do not reuse the `matches` table for tournament bracket fixtures.
- Do not implement `TournamentMatch extends Match`.
- Do not introduce React, Vue, Next.js, Tailwind, Vite, or a client-rendered app.
- Use `<c:url>` in JSPs. Do not use `${pageContext.request.contextPath}`.
- Add all user-facing copy to both message bundles.
- Add tests with each behavioral slice.
- Prefer full page POST/redirect/GET for v1. Partial updates can come later.
- Treat notifications as email-only for the first spine. The current app does
  not have a persisted in-app notification center.

## Phase 0: Product Decisions And Documentation Cleanup

Goal: make implementation decisions explicit before schema and services harden.

Tasks:

- Confirm the decisions in `feature-brief.md`.
- Treat the first implementation as the solo-registration spine.
- Keep team drafts in the expanded implementation.
- Use `BRACKET_SETUP` instead of the vague `LOCKED_BRACKET` name for new code.
- Decide migration numbering based on current latest Flyway migration.
- Use `/tournaments/{id}` as the public tournament detail URL.

Confirmed route convention:

- Public:
  - `GET /tournaments/{tournamentId}`
  - `POST /tournaments/{tournamentId}/solo-entry`
  - `POST /tournaments/{tournamentId}/solo-entry/leave`
  - `GET /tournaments/{tournamentId}/bracket`
- Host:
  - `GET /host/tournaments/new`
  - `POST /host/tournaments`
  - `GET /host/tournaments/{tournamentId}/edit`
  - `POST /host/tournaments/{tournamentId}`
  - `POST /host/tournaments/{tournamentId}/close-registration`
  - `POST /host/tournaments/{tournamentId}/bracket/generate`
  - `GET /host/tournaments/{tournamentId}/bracket/setup`
  - `POST /host/tournaments/{tournamentId}/bracket/publish`
  - `POST /host/tournaments/{tournamentId}/matches/{matchId}/winner`
  - `POST /host/tournaments/{tournamentId}/matches/{matchId}/walkover`
  - `POST /host/tournaments/{tournamentId}/cancel`

Acceptance criteria:

- The team agrees what belongs in the first spine.
- The route naming is stable.
- The under-capacity behavior is not left open.

## Phase 1: Domain Types And Persistence Foundation

Goal: add tournament tables and JPA entities without exposing UI yet.

### 1.1 Enums And Converters

Likely files:

- `models/src/main/java/ar/edu/itba/paw/models/types/TournamentStatus.java`
- `models/src/main/java/ar/edu/itba/paw/models/types/TournamentFormat.java`
- `models/src/main/java/ar/edu/itba/paw/models/types/TournamentTeamOrigin.java`
- `models/src/main/java/ar/edu/itba/paw/models/types/TournamentSoloEntryStatus.java`
- `models/src/main/java/ar/edu/itba/paw/models/types/TournamentMatchStatus.java`
- converter classes under `models/src/main/java/ar/edu/itba/paw/models/converters`

Recommended enum values:

- `TournamentStatus`: `REGISTRATION`, `BRACKET_SETUP`, `IN_PROGRESS`,
  `COMPLETED`, `CANCELLED`
- `TournamentFormat`: `SINGLE_ELIMINATION`
- `TournamentTeamOrigin`: `SOLO_POOL`, `TEAM_DRAFT`
- `TournamentSoloEntryStatus`: `IN_POOL`, `LEFT`, `ASSIGNED`, `UNASSIGNED`
- `TournamentMatchStatus`: `PENDING`, `SCHEDULED`, `AWAITING_RESULT`, `DONE`,
  `WALKOVER`

Tasks:

- Add enum converters following existing converter patterns.
- Keep Java enum constants uppercase, but persist database values as stable
  lowercase or lower snake case strings, matching the existing project enums.
- Add unit tests for converters if converter tests already exist.

### 1.2 Flyway Migrations

Likely path:

- `persistence/src/main/resources/db/migration`
- matching HSQLDB test migrations if the repository uses parallel test scripts

Tables for first spine:

- `tournaments`
- `tournament_teams`
- `tournament_team_members`
- `tournament_solo_entries`
- `tournament_matches`
- optional `tournament_round_defaults`

Suggested `tournaments` columns:

- `id BIGINT PRIMARY KEY`
- `host_user_id BIGINT NOT NULL`
- `sport VARCHAR(30) NOT NULL`
- `title VARCHAR(150) NOT NULL`
- `description TEXT`
- `address VARCHAR(255) NOT NULL`
- `latitude DOUBLE PRECISION`
- `longitude DOUBLE PRECISION`
- `starts_at TIMESTAMPTZ`
- `ends_at TIMESTAMPTZ`
- `price_per_player NUMERIC(10,2)`
- `banner_image_id BIGINT`
- `format VARCHAR(40) NOT NULL`
- `bracket_size SMALLINT NOT NULL`
- `team_size SMALLINT NOT NULL`
- `allow_solo_signup BOOLEAN NOT NULL`
- `allow_team_draft BOOLEAN NOT NULL`
- `registration_opens_at TIMESTAMPTZ NOT NULL`
- `registration_closes_at TIMESTAMPTZ NOT NULL`
- `status VARCHAR(40) NOT NULL`
- lifecycle timestamps listed in `feature-brief.md`
- soft-delete/cancel fields if matching existing event behavior
- `version BIGINT NOT NULL DEFAULT 0`

Suggested constraints:

- `bracket_size IN (4, 8, 16)`
- `team_size >= 1`
- at least one join mode enabled
- `registration_closes_at > registration_opens_at`

Suggested `tournament_teams` columns:

- `id`
- `tournament_id`
- `name`
- `origin`
- `seed_position`
- `created_at`
- `version`

Indexes/constraints:

- unique `(tournament_id, seed_position)` where `seed_position IS NOT NULL`
- index `(tournament_id)`

Suggested `tournament_team_members` columns:

- `id`
- `team_id`
- `user_id`
- `is_captain`
- `joined_at`
- `version`

Indexes/constraints:

- unique `(team_id, user_id)`
- index `(user_id)`

Suggested `tournament_solo_entries` columns:

- `id`
- `tournament_id`
- `user_id`
- `status`
- `assigned_team_id`
- `joined_at`
- `left_at`
- `version`

Indexes/constraints:

- unique `(tournament_id, user_id)`
- index `(tournament_id, status)`

Suggested `tournament_matches` columns:

- `id`
- `tournament_id`
- `round_number`
- `match_index`
- `team_a_id`
- `team_b_id`
- `winner_team_id`
- `scheduled_starts_at`
- `scheduled_ends_at`
- `address`
- `latitude`
- `longitude`
- `status`
- `parent_match_a_id`
- `parent_match_b_id`
- `created_at`
- `updated_at`
- `version`

Indexes/constraints:

- unique `(tournament_id, round_number, match_index)`
- index `(tournament_id, status)`
- index `(team_a_id)`
- index `(team_b_id)`
- index `(winner_team_id)`

Acceptance criteria:

- Migrations apply cleanly.
- Test database migrations apply cleanly.
- Referential integrity prevents orphaned tournament rows.

### 1.3 JPA Entities

Likely files:

- `models/src/main/java/ar/edu/itba/paw/models/Tournament.java`
- `models/src/main/java/ar/edu/itba/paw/models/TournamentTeam.java`
- `models/src/main/java/ar/edu/itba/paw/models/TournamentTeamMember.java`
- `models/src/main/java/ar/edu/itba/paw/models/TournamentSoloEntry.java`
- `models/src/main/java/ar/edu/itba/paw/models/TournamentMatch.java`

Tasks:

- Map entities with JPA annotations.
- Use lazy relationships by default.
- Add `@Version` to mutable entities.
- Keep entities simple: fields, getters, focused setters, no business workflows.
- Reuse `User`, `ImageMetadata`, and sport enum mappings.
- Do not create inheritance with `Match`.

Acceptance criteria:

- Entities compile.
- Basic persist/find tests pass.

### 1.4 DAO Contracts And Implementations

Likely files:

- `persistence-contracts/src/main/java/ar/edu/itba/paw/persistence/TournamentDao.java`
- `TournamentTeamDao.java`
- `TournamentSoloEntryDao.java`
- `TournamentMatchDao.java`
- JPA implementations under `persistence/src/main/java/ar/edu/itba/paw/persistence`

DAO methods for first spine:

`TournamentDao`:

- `Tournament create(...)`
- `Optional<Tournament> findById(long id)`
- `Optional<Tournament> findPublicById(long id)`
- `List<Tournament> findPublicRegistrationOrLive(...)` or paginated equivalent
- `List<Tournament> findHostedByUser(User host, ...)`
- `Tournament update(Tournament tournament)`

`TournamentSoloEntryDao`:

- `Optional<TournamentSoloEntry> findByTournamentAndUser(long tournamentId, long userId)`
- `List<TournamentSoloEntry> findActiveByTournament(long tournamentId)`
- `long countActiveByTournament(long tournamentId)`
- `TournamentSoloEntry create(...)`
- `TournamentSoloEntry update(...)`

`TournamentTeamDao`:

- `TournamentTeam create(...)`
- `List<TournamentTeam> findByTournament(long tournamentId)`
- `List<TournamentTeam> findSeededByTournament(long tournamentId)`
- `Optional<TournamentTeam> findUserTeam(long tournamentId, long userId)`
- `long countByTournament(long tournamentId)`

`TournamentMatchDao`:

- `TournamentMatch create(...)`
- `List<TournamentMatch> findByTournament(long tournamentId)`
- `Optional<TournamentMatch> findById(long matchId)`
- `Optional<TournamentMatch> findByTournamentAndId(long tournamentId, long matchId)`
- `List<TournamentMatch> findByTournamentAndRound(long tournamentId, int round)`
- `TournamentMatch update(TournamentMatch match)`

Persistence tests:

- create/find each entity
- unique seed position
- one solo entry per user per tournament
- fixture unique round/index
- query user's team
- query tournament bracket ordered by round and index

Acceptance criteria:

- DAO tests pass on HSQLDB.
- All repository methods needed by service phase exist.

## Phase 2: Service Contracts And Domain Rules

Goal: create service APIs and focused exceptions before controllers.

### 2.1 Request/Result Objects

Likely files:

- `CreateTournamentRequest`
- `UpdateTournamentRequest`
- `TournamentBracketView`
- `TournamentMatchScheduleRequest`
- `TournamentWinnerDeclarationRequest`
- `TournamentJoinFailureReason`
- `TournamentLifecycleFailureReason`
- service exceptions under `service-contracts/.../exceptions`

Request fields for create:

- title
- sport
- description
- address
- latitude/longitude
- starts/ends or play date range
- price
- banner image metadata id
- bracket size
- team size
- allow solo signup
- allow team draft
- registration opens/closes
- round defaults if added in first spine

Tasks:

- Keep request classes immutable where practical.
- Put validation decisions in services, not controllers only.
- Include acting user in mutating service calls.

Acceptance criteria:

- Contract module compiles.
- Error reason enums are specific enough for controller messages.

### 2.2 TournamentService

Responsibilities:

- create tournament directly in registration
- find public tournament
- find host tournament
- update editable fields
- cancel tournament
- lifecycle guard helpers

Suggested methods:

- `Tournament createTournament(User host, CreateTournamentRequest request)`
- `Optional<Tournament> findPublicTournament(long tournamentId)`
- `Optional<Tournament> findTournamentForHost(long tournamentId, User host)`
- `Tournament update(long tournamentId, User actingUser, UpdateTournamentRequest request)`
- `Tournament cancel(long tournamentId, User actingUser, String reason)`

Rules:

- Only host or admin/mod can mutate.
- Public read allows `REGISTRATION`, `BRACKET_SETUP`, `IN_PROGRESS`,
  `COMPLETED`, and `CANCELLED` as appropriate.
- Structural fields cannot change after creation.
- Completed tournaments are read-only.

Tests:

- host can create a valid tournament in registration
- create fails with invalid bracket size/team size/window
- non-host cannot update/cancel
- structural update after creation fails
- cancel fails after completed
- admin/mod path if current auth model supports it

### 2.3 TournamentRegistrationService

Responsibilities for first spine:

- join solo pool
- leave solo pool
- query user's participation
- close registration
- form teams from solo entries
- cancel under-capacity tournaments

Suggested methods:

- `TournamentSoloEntry joinSolo(long tournamentId, User user)`
- `void leaveSolo(long tournamentId, User user)`
- `Optional<TournamentSoloEntry> findSoloEntry(long tournamentId, User user)`
- `Optional<TournamentTeam> findUserTeam(long tournamentId, User user)`
- `Tournament closeRegistration(long tournamentId, User actingUser)`

Rules:

- Join requires tournament status `REGISTRATION`.
- Join is idempotent for an existing `IN_POOL` entry.
- Leave requires status `REGISTRATION`.
- Leave marks entry `LEFT`; it does not delete the row.
- A user cannot have an active solo entry and be on a team.
- When closing registration:
  - collect active solo entries
  - create complete `SOLO_POOL` teams of exactly `team_size`, capped at
    `bracket_size`
  - assign solo entries to teams
  - mark leftovers and overflow entries `UNASSIGNED`
  - if complete team count is below `bracket_size`, cancel tournament
  - otherwise transition to `BRACKET_SETUP`

Tests:

- join solo succeeds
- join solo while logged in twice is idempotent
- leave solo succeeds
- leave solo after registration closes fails
- close registration groups exact multiple into teams
- close registration marks leftovers unassigned
- close registration marks overflow entries unassigned when the solo pool
  exceeds bracket capacity
- close registration under capacity cancels tournament
- user already on team cannot join solo
- non-host cannot close registration

### 2.4 TournamentBracketService

Responsibilities:

- generate bracket
- schedule round one
- publish bracket
- load bracket view
- declare winner
- record walkover
- propagate winner
- complete tournament

Suggested methods:

- `List<TournamentMatch> generateBracket(long tournamentId, User actingUser)`
- `Tournament publishBracket(long tournamentId, User actingUser, List<TournamentMatchScheduleRequest> schedules)`
- `TournamentBracketView getBracket(long tournamentId, User viewer)`
- `TournamentMatch declareWinner(long tournamentId, long matchId, long winnerTeamId, User actingUser)`
- `TournamentMatch recordWalkover(long tournamentId, long matchId, long forfeitingTeamId, User actingUser)`

Bracket generation rules:

- Tournament must be `BRACKET_SETUP`.
- Team count must equal `bracket_size`.
- There must be no existing bracket unless explicitly regenerating before
  publication is supported.
- Initial seed order can be random or stable by team creation order for first
  implementation. Stable order is easier to test.
- Create all fixtures upfront.
- Round one gets teams.
- Later rounds get parent references and null teams.

Publishing rules:

- Round-one fixtures must all have date/time and venue.
- Status changes to `IN_PROGRESS`.
- `started_at` is set.
- Players can now see opponents.

Winner declaration rules:

- Tournament must be `IN_PROGRESS`.
- Fixture must have both teams.
- Fixture must not already have a winner.
- Winner must be `team_a` or `team_b`.
- Winner propagates to the proper child fixture slot.
- If child fixture now has both teams, it becomes `SCHEDULED`.
- If final fixture gets a winner, tournament becomes `COMPLETED`.

Walkover rules:

- Same as winner declaration, but host selects forfeiting team.
- Non-forfeiting team advances.
- Fixture status is `WALKOVER`.

Tests:

- generates correct fixture count for 4/8/16
- no duplicate teams in round one
- parent links point to child slots correctly
- cannot generate under capacity
- cannot publish bracket with missing schedule
- cannot declare winner before bracket is in progress
- cannot declare winner for unknown team
- winner propagates to child team A
- winner propagates to child team B
- final declaration completes tournament
- walkover advances non-forfeiting team

Implemented backend status:

- `TournamentBracketServiceImpl` now implements generation, publication,
  bracket loading, winner declaration, walkovers, propagation, completion, and
  host/admin authorization checks.
- Service tests cover generation sizes, no duplicate round-one teams, parent
  links, under-capacity and lifecycle guards, duplicate generation, non-host
  denial, admin/mod mutation, publication schedules, bracket read
  authorization, viewer focus, propagation, final completion, walkovers, and a
  service-level bracket run from generation to completion.
- Web controllers, JSPs, routes, UI messages, and notification dispatch remain
  future phases.

## Phase 3: Host Create And Publish UI

Goal: hosts can create tournaments from server-rendered pages.

Likely files:

- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/CreateTournamentForm.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/UpdateTournamentForm.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/HostTournamentController.java`
- `webapp/src/main/webapp/WEB-INF/views/host/tournaments/create.jsp`
- `webapp/src/main/webapp/WEB-INF/views/host/tournaments/edit.jsp`
- `webapp/src/main/webapp/css/tournaments.css`

Validation:

- title required and max length
- sport required
- address required
- bracket size one of 4/8/16
- team size at least 1
- at least one join mode true
- registration close after open
- registration window at least 24h if product still wants that rule

Controller rules:

- Use authenticated current user from existing security utilities.
- Delegate business validation to services.
- Convert service exceptions to localized field/global errors.
- Keep controller thin.

Message bundle examples:

- `tournament.create.title`
- `tournament.form.title.label`
- `tournament.form.bracketSize.label`
- `tournament.form.teamSize.label`
- `tournament.form.registrationOpens.label`
- `tournament.form.registrationCloses.label`
- `tournament.form.submit.create`
- validation error keys for every service failure reason

Tests:

- GET create requires auth
- POST create with valid data redirects to detail/setup
- POST invalid data returns form
- non-host cannot edit another host's tournament
- Spanish error/copy path if controller prepares localized labels

Acceptance criteria:

- Host can create a valid tournament in `REGISTRATION`.
- Invalid form submissions preserve user input and show localized errors.

## Phase 4: Feed And Public Detail Integration

Goal: tournaments become discoverable without breaking match feed behavior.

Recommended first implementation:

- Add a tournament query and a feed item view model that can wrap either a
  `Match` or `Tournament`.
- If mixed pagination is too expensive at first, implement `?type=tournament`
  first and merge all-feed behavior later.
- Add a tournament card JSP tag that uses the same visual chrome as match cards.

Likely files:

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/TournamentController.java`
- `webapp/src/main/webapp/WEB-INF/views/tournaments/detail.jsp`
- `webapp/src/main/webapp/WEB-INF/tags/tournamentBadge.tag`
- `webapp/src/main/webapp/WEB-INF/tags/tournamentLifecycleBadge.tag`
- `webapp/src/main/webapp/WEB-INF/tags/tournamentCard.tag`

Detail page sections:

- banner/title/sport/status
- registration close time
- bracket size/team size
- venue
- price if present
- join solo CTA if allowed and user can join
- leave solo/manage participation state if already joined
- team forming count
- host information

Tests:

- public detail renders for a registration tournament
- logged-out join redirects to login
- cancelled/completed public behavior matches decision

Acceptance criteria:

- A player can discover a tournament and understand whether they can join.

## Phase 5: Solo Registration UI

Goal: authenticated players can join/leave the solo pool.

Controller actions:

- `POST /tournaments/{id}/solo-entry`
- `POST /tournaments/{id}/solo-entry/leave`

Tasks:

- Add CSRF-safe forms/buttons in detail page.
- Add success/error flash messages.
- Add localized copy for joined, left, already participating, registration
  closed, and under-capacity caveat.
- Update detail view model with:
  - `canJoinSolo`
  - `canLeaveSolo`
  - `participationStatus`
  - `soloPoolCount`
  - `lockedTeamCount`

Tests:

- logged-in user can join
- logged-out user cannot join
- duplicate join does not error
- leave works during registration
- leave hidden/denied after registration closes
- user in team cannot join solo

Acceptance criteria:

- Solo registration works end to end from the public detail page.

## Phase 6: Close Registration And Team Formation

Goal: host can close registration and lock teams.

Host route:

- `POST /host/tournaments/{id}/close-registration`

Tasks:

- Add host-only CTA on detail or host dashboard.
- Implement service transaction for close/group/cancel.
- Create up to `bracket_size` solo-pool teams with names like `Solo squad #1`.
- Mark leftovers and overflow solo entries `UNASSIGNED`.
- Transition to `BRACKET_SETUP` when enough teams exist.
- Transition to `CANCELLED` when fewer than `bracket_size` teams exist.
- Add localized cancellation/under-capacity messages.

Tests:

- host close succeeds with exact capacity
- close with extras marks leftovers unassigned
- close with more complete solo teams than bracket slots marks overflow entries
  unassigned
- close under capacity cancels
- non-host close denied
- close twice is idempotent or fails predictably

Acceptance criteria:

- After close, teams are fixed and registration actions are unavailable.

## Phase 7: Bracket Generation And Host Setup

Goal: host can generate and inspect a valid bracket.

Routes:

- `POST /host/tournaments/{id}/bracket/generate`
- `GET /host/tournaments/{id}/bracket/setup`

Tasks:

- Implement bracket generation.
- Add bracket setup view model grouped by round.
- Add JSP tag/fragment for bracket grid.
- Show teams, round names, and fixture statuses.
- For first implementation, seed by stable creation order or explicit random
  shuffle with deterministic test hook.
- Do not implement drag-swap yet unless this phase is already stable.

Tests:

- host can generate after registration close
- players cannot see unpublished bracket if product wants seeding hidden
- bracket setup page denied to non-host

Acceptance criteria:

- Host can generate a bracket and see all fixtures in the setup page.

## Phase 8: Round-One Scheduling And Bracket Publication

Goal: host publishes a playable bracket.

Route:

- `POST /host/tournaments/{id}/bracket/publish`

Tasks:

- Add schedule form for each round-one fixture.
- Default address/coordinates to tournament venue.
- Validate every round-one fixture has start time and venue.
- Persist schedule fields.
- Transition to `IN_PROGRESS`.
- Set `started_at`.
- Dispatch minimum bracket-published notification after transaction.

Tests:

- missing schedule rejected
- successful publish transitions status
- public bracket visible after publish
- host cannot publish without generated bracket

Acceptance criteria:

- Players can see their first opponent and schedule.

## Phase 9: Public/Player Bracket View

Goal: players and public users can view the bracket once it is in progress.

Route:

- `GET /tournaments/{id}/bracket`

Tasks:

- Build `TournamentBracketViewModel`.
- Group matches by round.
- Compute selected/default focused match:
  - host: earliest fixture without winner
  - player on active team: earliest unplayed fixture for their team
  - eliminated player: earliest unplayed fixture overall
  - public viewer: earliest unplayed fixture overall
- Highlight viewer's team when logged in and participating.
- Add match focus rail as JSP fragment/tag.
- Add manual refresh button or regular page reload behavior.

Tests:

- bracket unavailable before `IN_PROGRESS` to regular players
- bracket visible in `IN_PROGRESS`
- participant team is identified in view model
- completed bracket remains visible

Acceptance criteria:

- Players can understand where they are in the tournament.

## Phase 10: Host Winner Declaration And Walkovers

Goal: host can run the tournament to completion.

Routes:

- `POST /host/tournaments/{id}/matches/{matchId}/winner`
- `POST /host/tournaments/{id}/matches/{matchId}/walkover`

Tasks:

- Add winner declaration form/modal/page in host bracket rail.
- Add confirmation screen or modal text.
- Add walkover action where host selects forfeiting team.
- Persist result.
- Propagate winner.
- Complete final.
- Dispatch result/completion notifications after transaction.
- Redirect back to bracket.

Tests:

- host can declare winner
- non-host cannot declare winner
- cannot declare winner for pending fixture with missing team
- cannot declare winner twice
- cannot declare team outside fixture
- child fixture receives winner
- final winner completes tournament
- walkover stores `WALKOVER` and advances other team

Acceptance criteria:

- An 8-team tournament can be completed end to end.

## Phase 11: Minimum Notifications

Goal: deliver the essential email notifications without implementing the entire
long-term notification catalog.

Likely files:

- `service-contracts/.../TournamentNotificationService.java`
- `services/.../TournamentNotificationServiceImpl.java`
- mail template renderer additions
- mail templates under `services/src/main/resources/mail`
- message bundle entries

Minimum notification methods:

- `notifyBracketPublished(Tournament tournament)`
- `notifyMatchResult(Tournament tournament, TournamentMatch match, TournamentTeam winner, TournamentTeam loser)`
- `notifyWalkover(Tournament tournament, TournamentMatch match, TournamentTeam advancingTeam, TournamentTeam forfeitingTeam)`
- `notifyTournamentCompleted(Tournament tournament, TournamentTeam champion)`
- `notifyTournamentCancelled(Tournament tournament)`

Tasks:

- Find all recipient users through team memberships.
- Deduplicate recipients.
- Use recipient preferred locale.
- Send templated email.
- Do not log personal email bodies or sensitive data.

Tests:

- notifications deduplicate users
- recipient locale is used
- completed sends champion and participant variants
- no notification sent to unrelated users

Acceptance criteria:

- Essential tournament state changes notify affected players.

Do not implement a tournament-specific in-app notification shortcut. If in-app
notifications become required, add a generic notification foundation first:

- `Notification` entity/table with recipient, type, title/body, target URL,
  read timestamp, created timestamp, and metadata payload if needed
- DAO/service APIs for create, list unread/recent, mark read, and count unread
- header badge/dropdown integration
- authorization so users can only read their own notifications
- tests for unread counts, mark-read behavior, and locale-safe display

## Phase 12: Authorization And Security Hardening

Goal: make allowed/denied paths explicit.

Tasks:

- Add route-level Spring Security rules if needed.
- Centralize tournament ownership/admin checks in a policy/helper or service.
- Confirm logged-out read paths work.
- Confirm logged-out mutating paths redirect to login.
- Confirm non-host mutating paths fail with 403 or redirect pattern consistent
  with current app.
- Confirm admin/mod behavior if existing role model is ready.

Tests:

- public detail allowed anonymously
- join requires auth
- host pages require auth
- non-host denied from host tournament actions
- admin/mod allowed if implemented

Acceptance criteria:

- Security behavior is predictable and tested.

## Phase 13: Team Drafts Expanded Slice

Start this only after the solo spine works.

### 13.1 Data Model

Add:

- `TournamentTeamDraft`
- `TournamentDraftInvite`

Enums:

- `TournamentTeamDraftStatus`: `OPEN`, `LOCKED`, `DISBANDED`
- `TournamentDraftInviteStatus`: `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`

Migrations:

- draft table
- invite table
- unique active invite constraints where feasible
- created team FK for locked drafts

### 13.2 Service Behavior

Add to `TournamentRegistrationService` or a dedicated draft service:

- start draft
- invite user
- accept invite
- decline invite
- cancel invite/swap
- disband draft
- lock draft when accepted count reaches team size

Rules:

- Invite only existing users.
- Captain cannot invite self.
- User can be in at most one active participation path.
- Accepting invite while in solo pool removes solo entry in same transaction.
- Accepting invite while already on another team/draft fails.
- Locking draft creates `TournamentTeam` and `TournamentTeamMember` rows once.
- Race protection uses transaction plus optimistic locking.

Tests:

- captain starts draft
- invitee accepts
- invitee declines
- duplicate accept is idempotent or fails predictably
- final accept locks team
- simultaneous final accepts create one team only
- disband notifies and releases participants
- accepting while in solo removes solo entry
- accepting while on another team fails

### 13.3 UI

Views:

- new draft page
- draft status page
- invite review page
- captain roster management

Notifications:

- draft invite sent
- invite accepted/declined
- team locked
- draft disbanded

Acceptance criteria:

- A captain can form a complete team through invites.
- The locked draft consumes one bracket slot.

## Phase 14: Feed, My Events, And Player-State Polish

Goal: make tournaments feel first-class in the application.

Tasks:

- Add feed type filter chip.
- Add tournament cards to search/discovery.
- Add My Events `Tournaments` tab.
- Add next-match callout.
- Add advanced/eliminated/champion states.
- Add review CTA only after tournament completion and only when review rules
  allow it.

Tests:

- joined tournaments appear for participant
- completed tournaments remain available
- next-match callout hidden when eliminated
- champion view only for winning team
- review CTA respects existing review eligibility

Acceptance criteria:

- Players can return to tournaments naturally after joining.

## Phase 15: Optional Automation

Goal: add scheduled behavior after manual flows are reliable.

Tasks:

- Add scheduled job to close registration at `registration_closes_at`.
- Add 24-hour closing reminder.
- Add job or query-derived state for `AWAITING_RESULT` when scheduled time has
  passed.
- Ensure jobs are idempotent.
- Ensure jobs use service methods rather than direct DAO manipulation.
- Add logging with stable IDs only.

Tests:

- job closes eligible registration tournaments
- job does not close drafts/cancelled/completed tournaments
- job handles already-closed tournament idempotently
- reminder sends once

Acceptance criteria:

- Automation improves UX without changing manual service rules.

## Phase 16: Reseeding And Host UX Enhancements

Goal: add bracket setup polish after core correctness.

Tasks:

- Add seed swap service method.
- Add reshuffle service method.
- Add undo stack only for pre-publication seeding if still desired.
- Add desktop drag/drop and touch-friendly fallback buttons.
- Keep reseeding disabled after `IN_PROGRESS`.

Tests:

- swap preserves unique seeds
- reshuffle preserves all teams exactly once
- reseeding denied after publication

Acceptance criteria:

- Host can adjust seeds before publishing the bracket.

## Phase 17: Future Admin Correction Tools

Goal: handle rare result mistakes without a fragile 30-second undo.

Tasks:

- Design admin-only result correction.
- Recalculate downstream bracket carefully or restrict correction before any
  child fixture has a declared winner.
- Notify affected users if correction changes match outcomes.

This is not part of the first spine.

## End-To-End Smoke Scenario

Use this scenario repeatedly while implementing:

1. Host creates 8-team tournament with team size 1 or 2 for easy testing.
2. Eight players join solo.
3. Host closes registration.
4. System creates eight solo teams.
5. Host generates bracket.
6. Host schedules round one.
7. Host publishes bracket.
8. Player opens bracket and sees first match.
9. Host declares four round-one winners.
10. Host declares two semifinal winners.
11. Host declares final winner.
12. Tournament becomes completed.
13. Champion view/status appears.
14. Completed bracket remains readable.

## Test Matrix By Layer

Persistence:

- migrations
- entity mappings
- unique constraints
- DAO queries

Services:

- lifecycle transitions
- registration exclusivity
- solo grouping
- bracket generation
- winner propagation
- notification recipient selection

Web:

- public read
- authenticated join/leave
- host-only actions
- localized errors
- JSP rendering for major states

Security:

- anonymous allowed routes
- anonymous denied routes
- host ownership
- admin/mod override where implemented

I18n:

- English and Spanish bundle entries for all new copy
- locale-aware mail templates
- no hardcoded visible copy in controllers/services/JSPs

## First Spine Checklist

- [x] Add tournament enums and converters.
- [x] Add Flyway migrations for first-spine tables.
- [x] Add JPA entities.
- [x] Add DAO contracts and implementations.
- [x] Add persistence tests.
- [x] Add service contracts, request objects, exceptions, and failure reasons.
- [x] Implement `TournamentService`.
- [x] Implement `TournamentRegistrationService`.
- [x] Implement `TournamentBracketService`.
- [x] Add service tests for lifecycle, registration, grouping, generation, and propagation.
- [x] Add host create form, controller, JSP, and messages.
- [x] Add public tournament detail page.
- [x] Add solo join/leave actions.
- [x] Add host close-registration action.
- [x] Add bracket generation host page.
- [x] Add bracket publication with round-one schedule.
- [ ] Add public/player bracket page.
- [ ] Add host winner/walkover actions.
- [ ] Add minimum notification service and templates.
- [ ] Add controller/security tests.
- [ ] Run `mvn test`.
- [ ] Run Spotless if formatting fails.
