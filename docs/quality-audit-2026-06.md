# Quality Audit & Remediation Plan — June 2026

Audit of `dev` at commit `ceb23b59` against the recurring faculty correction patterns
from the TP1/TP2 devoluciones. Two passes were run: a full-codebase audit and a
focused re-audit of the tournament-matches-dashboard merge (PR #75,
`84e981d0..ceb23b59`).

- **Part 1 — Findings**: every confirmed issue, with file/line references and the
  faculty correction category it maps to.
- **Part 2 — Clean areas**: what was checked and found compliant (useful for the
  defensa).
- **Part 3 — Remediation plan**: workstreams, dependency order, and a
  parallelization scheme.

Severity scale: **P1** = likely marked as conceptual/grave · **P2** = likely marked,
cheap points · **P3** = may be marked / polish.

---

## Part 1 — Findings

### P1-1. Host edit GET endpoints have no ownership check and 500 on bad id

**Faculty category:** _Chequeos de ownership ausentes / input inválido produce 500_

| Location | Problem |
|---|---|
| `webapp/.../controller/HostTournamentController.java:154-162` (`showEditTournament`) | Loads via `tournamentService.findPublicTournament(id).orElse(null)` and renders the edit form. Any authenticated user who knows the URL sees the pre-filled edit form of someone else's tournament. |
| `webapp/.../controller/HostTournamentController.java:173-175` (`updateTournament`) | Same `findPublicTournament(...).orElse(null)` used to build the form config before the service ownership check runs. |
| `webapp/.../controller/HostController.java:133-140` (`showEditEvent`) | Identical pattern with `matchService.findMatchById(matchId).orElse(null)`. |
| Both controllers | `.orElse(null)` followed by `toForm(...)` / `editFormConfig(...)` → **NPE → 500** for a nonexistent id, instead of 404. |

The POST paths are safe — `TournamentServiceImpl.update` (line 292) calls
`validateCanMutate(tournament, actingUser)` — but the GET/POST asymmetry is exactly
what gets flagged. Note `findPublicTournament` is also used the same way at
`HostTournamentController.java:292, 338, 380`.

**Fix:** add `findTournamentForHost(id, actingUser)` / `findMatchForHost(id, actingUser)`
to the services (throwing the existing `TournamentNotFoundException` /
`MatchNotFoundException` and `TournamentForbiddenActionException` /
`MatchForbiddenActionException`, reusing `validateCanMutate`-style logic), and use
them in every host-scoped handler. `AccessExceptionHandler` already maps
`NotFoundException`/`ForbiddenException` to 404/403 pages, so no webapp plumbing is
needed.

### P1-2. Manual ownership check in a controller

**Faculty category:** _Chequeos de ownership hechos manualmente en controllers_

- `webapp/.../controller/UserModerationReportController.java:126` —
  `if (report.getReporter() == null || !report.getReporter().getId().equals(user.getId())) throw new ResponseStatusException(NOT_FOUND)`.

**Fix:** move into `ModerationService` as `findReportForReporter(reportId, user)`
throwing `ModerationReportNotFoundException`; controller just calls it.

### P1-3. Flyway version gap + test/production migration divergence

**Faculty category:** _Falta de números de versión / migraciones test-prod desalineadas_
(also explicitly required by `AGENTS.md` §"Build And Repository Hygiene")

- Production: **V16 is missing** (`V15__add_declined_invite_status.sql` →
  `V17__add_recurring_match_series.sql`).
- Test (`persistence/src/test/resources/db/testmigration/`): own renumbered sequence
  that partially mirrors production (test V8 = prod V11), with gaps V16–V21 and
  V23–V29, then realigns at V30+. 21 test migrations vs 37 production.

**Fix:** production numbering cannot be rewritten where databases already applied it
(checksum/repair pain on pampero) — fill the hole with a no-op
`V16__reserved_noop.sql` (comment-only) so the sequence is contiguous. For tests,
renumber the HSQLDB migrations to match production version numbers 1:1, adding
no-op files for production versions that are PostgreSQL-only (sequence alignments
etc.), so both trees show the same contiguous sequence.

### P1-4. JDK exceptions used as business exceptions (≈30 sites)

**Faculty category:** _Excepciones del JDK como si fueran de negocio / magic strings
en mensajes de excepción_

A rich domain hierarchy already exists in `models/.../exceptions/` (`DomainException`,
`NotFoundException`, `ForbiddenException` + ~90 per-area subtypes), yet services throw
raw `IllegalArgumentException` with an i18n key (or hardcoded English) as message:

| File | Lines | Notes |
|---|---|---|
| `services/.../MatchServiceImpl.java` | 88, 1256, 1259, 1272, 1305, 1311, 1314, 1323, 1331, 1337, 1359, 1376 | 1376 throws `IAE("match.create.error.capacityAboveMax")` although `MatchUpdateCapacityAboveMaxException` already exists. 1305-1337 are recurrence rules → need a small `MatchRecurrence*Exception` family or reuse `MatchUpdateException` subtypes. 1256/1259 are pagination → see note below. |
| `services/.../MatchParticipationServiceImpl.java` | 99, 143, 345, 758 | all `IAE("exception.user.notNull")` — null-guard, see note below. |
| `services/.../TournamentServiceImpl.java` | 442, 448, 478 | `"invalidRequest"` magic string. |
| `services/.../TournamentRegistrationServiceImpl.java` | 337 | null-guard. |
| `services/.../UserServiceImpl.java` | 138 | null-guard. |
| `services/.../ModerationServiceImpl.java` | 494 | null-guard. |
| `services/.../MatchReservationServiceImpl.java` | 396 | hardcoded English `"User must not be null."` — inconsistent with the i18n-key style used elsewhere. |
| `services/.../UserSportRatingServiceImpl.java` | 86, 102, 106, 112 | hardcoded English messages; 112 concatenates the sport value. |

Notes:
- **Null-guards** (`exception.user.notNull`): a null authenticated user is a
  programming error, not a business outcome. Either keep them as JDK exceptions with
  a *non-i18n* message (defensible — document the convention), or route them to a
  domain `InvalidUserException` (already exists in `models/.../exceptions/user/`).
  Pick ONE convention and apply it everywhere.
- **Pagination** (`pagination.error.invalidPage`): invalid page/size should surface
  as a controlled 400. A small `InvalidPaginationException extends DomainException`
  handled centrally as 400 closes the "input inválido → 500" hole too.

Related (same family):
- `webapp/.../exception/VerificationExceptionHandler.java:16` and
  `PasswordResetExceptionHandler.java:17,23` build codes via
  `"prefix." + exception.getMessage()`. The project-wide convention is "domain
  exception's `getMessage()` is a message-code suffix"; it works, but is implicit.
  **Optional hardening:** add `getMessageCode()` to `DomainException` and switch the
  `"prefix." + getMessage()` call sites (also in `TournamentController`,
  `ModerationReportController`, `HostController`, `PlayerParticipationController`)
  to it — same behavior, explicit contract, easy to defend.

### P2-1. Empty `messages_en.properties`

- `webapp/src/main/resources/i18n/messages_en.properties` is a versioned **0-byte
  file** (English lives in the default `messages.properties`). Faculty list calls
  out redundant bundle files verbatim. **Fix: delete it.** Verify nothing references
  the `en` bundle explicitly (the default resolves for English).

### P2-2. Hardcoded "vs" separator in the new tournament match card

- `webapp/.../WEB-INF/tags/tournamentMatchCard.tag:63` — literal ` vs ` between the
  two team-name `<c:choose>` blocks; everything else in the tag is localized.
  **Fix:** `<spring:message code="tournament.match.versus"/>` + key in **both**
  bundles.

### P2-3. Magic strings in `MatchDashboardPageSupport`

- `webapp/.../controller/MatchDashboardPageSupport.java:397` —
  `List.of("football", "tennis", "basketball", "padel", "other")` re-declares the
  `Sport` enum as string literals (goes stale silently). Derive from the enum.
- Same file, lines `352, 388, 427, 435, 444, 471` — filter state compared/emitted as
  literal `"PAST"` (7+ occurrences). Use the existing filter enum / a constant.
- Same file ~line 794 — `// TODO: is this necessary? is there a better way` on the
  CSV-encoding helper; lines ~260-291 — three private filter-option builders declare
  unused `path`/`searchQuery`/`sort` parameters. Resolve the TODO, drop dead params.
- File is 1,217 lines and faculty counts controller-support classes as controller
  code. Content is presentation-only (verified), but consider splitting the
  tournament-matches block from the matches block if touched again. (Stretch.)

### P2-4. Missing `equals`/`hashCode` by id on JPA entities

**Faculty category:** _Falta de equals/hashCode por ID (rompe Set/Map/contains)_

Have it: `User`, `MatchSeries`. Missing it (15): `Match`, `Tournament`,
`TournamentTeam`, `TournamentTeamMember`, `TournamentMatch`, `TournamentSoloEntry`,
`MatchParticipant`, `PlayerReview`, `UserBan`, `UserAccount`, `UserSportRating`,
`ModerationReport`, `EmailActionRequest`, `Image`, `ImageMetadata`.

**Fix:** mechanical — id-based `equals`/`hashCode` following the existing `User`
implementation (`models/.../User.java:129-146`). Guard for null id (transient
entities).

### P2-5. Test-quality items (testing was heavily marked in past devoluciones)

1. **`assertDoesNotThrow` as the only assertion** —
   `MatchParticipationServiceImplTest.java:181, 201, 256`;
   `MatchReservationServiceImplTest.java:66, 86, 256, 292`;
   `AsyncMailDispatchServiceTest`; `UserBanJpaDaoTest.java:302`.
   Replace with assertions on resulting state / returned values.
2. **`Mockito.spy` on the class under test** — `MatchServiceImplTest.java:80` (and
   spied `MatchNotificationServiceImpl` at 68-70). Restructure to mock collaborators
   only.
3. **Wall-clock in tests** — `UserBanJpaDaoTest.java:27-29, 291, 293, 315`;
   `TournamentMatchJpaDaoTest.java:67`; `TournamentJpaDaoTest.java:930, 955` use
   `Instant.now()`. Use fixed instants (service tests already do `Clock.fixed`).
4. **Zero `@ParameterizedTest` in 62 test classes** with visible copy-paste families:
   `UserBanJpaDaoTest` (~6 ban-state variants), `TournamentMatchJpaDaoTest`
   `countByUserParticipant*` (~459-483), `TournamentServiceImplTest` `createFailsWith*`
   (5 consecutive), `MatchJpaDaoTest` `testFind*` (~14), `UserJpaDaoTest:379-396`.
   Convert at least the obvious families.
5. **Coverage gaps** — no tests for the internal `*DataServiceImpl` classes
   (`MatchDataServiceImpl`, `UserDataServiceImpl`, `TournamentDataServiceImpl`,
   `TournamentTeamDataServiceImpl`, `PlayerReviewDataServiceImpl`,
   `MatchParticipantDataServiceImpl`); **no test exercising LIKE wildcard escaping**
   even though the escaping is implemented; thin negative-path coverage on
   controllers (403/404 on host routes — will be added naturally by P1-1's tests).

### P3 (smaller / defensible — fix opportunistically)

| Item | Location | Note |
|---|---|---|
| Boxed primitive on NOT NULL column | `models/.../Image.java:30` (`Long contentLength`, `nullable=false`) | → `long`. |
| Utility class not final | `services/.../utils/DistanceUtils.java` | `public final class` + private ctor (webapp utils already do this). |
| 11 `TODO`s in controllers | `EventPageSupport.java:565,573` (string host-actions → enum), `HostTournamentController.java:259` ("reason not localized"), `FeedController.java:101` (unused param), `AuthController.java:53`, `HostParticipationController.java:66` (unused method), image-handling TODOs in `HostController.java:419` / `AccountController.java:108` / `HostTournamentController.java:634`, `EventController.java:127`, `MatchDashboardPageSupport.java:~794` | Resolve or delete before hand-in — visible TODOs admitting known issues invite marks. |
| `MatchServiceImpl` 1,445 lines | `services/.../MatchServiceImpl.java` | Stretch: extract recurrence validation (the 1272-1337 block) into a helper — pairs naturally with P1-4. |
| Enum converter nulls invalid input | `webapp/.../config/converters/StringToTournamentMatchStatusConverter.java` (+17 siblings) | Invalid filter → treated as "no filter" instead of 400. Consistent project convention; **document as deliberate**, no change needed. |
| `ModerationServiceImpl` injects `UserBanDao` + `ModerationReportDao` | `services/.../ModerationServiceImpl.java:48-50` | Defensible if moderation (reports + bans) is one aggregate — **be ready to justify in the defensa**, or split a `UserBanService`. No change planned. |
| In-memory seeding sort | `services/.../TournamentBracketServiceImpl.java:508-520` | Sorts bracket teams by Java-computed average Elo; bounded by bracket size. Defensible — prepare the justification. |

---

## Part 2 — Clean areas (verified, keep it that way)

- **Security:** remember-me key externalized with placeholder rejection
  (`RememberMeKey.java`); no credentials in git (example properties + gitignore);
  mail base URL from config; consistent `<c:out>` escaping; LIKE wildcards escaped
  with `ESCAPE` in `MatchJpaDao`, `TournamentJpaDao`, `TournamentMatchJpaDao`; no
  `@PreAuthorize` — rules centralized in `SecurityConfig` ending in
  `anyRequest().authenticated()`; passwords/tokens excluded from `toString()`; CSRF
  tokens in all forms; secure cookie flags.
- **Architecture:** webapp depends only on contracts; services never return view
  names; class-level `@Transactional(readOnly = true)` + per-method `@Transactional`
  on writes across the main services.
- **Persistence:** all relations LAZY; 1+1 ID-first pagination with fetch joins in
  the new `TournamentMatchJpaDao` (no N+1); explicit `COUNT` queries (no
  `replaceFirst`); `setFirstResult`/`setMaxResults`; `ImageMetadata` avoids loading
  image bytes; parenthesized AND/OR; full parameter binding; no production JDBC.
- **POMs / repo:** versions and scopes managed in the parent; models framework-light;
  HSQLDB test-scoped; servlet-api provided; JUnit 5 only; no IDE files/artifacts
  committed; `ThreadPoolTaskExecutor`s configured with shutdown wait +
  `CallerRunsPolicy`.
- **i18n:** both bundles key-complete (including the 7 new dashboard keys); mails use
  the recipient's stored language, not `LocaleContextHolder`; dynamic `<html lang>`;
  parameterized messages; `tf:` date functions used in new views.
- **Logging:** logback with rotation, WARN root / INFO app logger;
  `logback-test.xml` in all three test modules; nothing sensitive logged.
- **New controller code (PR #75):** typed binding via validated `SearchForm`
  (`@Min` page, `@Size`/`@Pattern` query), controlled 400s, URLs via
  `UriComponentsBuilder`/`<c:url>`+`<c:param>`, controller tests assert view names
  and model attributes (no text-fragment coupling).

---

## Part 3 — Remediation plan

### Workstreams

Independent tracks (different files, no logical coupling) — they can run **in
parallel**. Inside each track, steps are ordered by dependency.

```
Track A (ownership/404)      Track B (exceptions)        Track C (migrations)
  A1 service methods           B1 inventory + mapping      C1 V16 no-op
  A2 controllers use them      B2 replace IAE/ISE          C2 renumber test migrations
  A3 report ownership → svc    B3 (opt) getMessageCode()   C3 green: mvn test (persistence)
  A4 tests (allow+deny+404)    B4 update service tests

Track D (test quality)       Track E (dashboard+i18n)    Track F (mechanical)
  D1 fix Instant.now()          E1 "vs" message key         F1 equals/hashCode ×15
  D2 kill spy / addt'l asserts  E2 sports list → enum       F2 Image.contentLength long
  D3 parameterize families      E3 "PAST" → enum/constant   F3 DistanceUtils final
  D4 LIKE-escape + DataService  E4 dead params + TODO       F4 delete messages_en
     tests                      E5 sweep remaining TODOs    F5 spotless:apply
```

**Cross-track dependencies (the only ones):**

1. **B before the affected parts of D.** B4 rewrites service-test expectations
   (`assertThrows(IllegalArgumentException)` → domain types); doing D's edits on the
   same test classes first creates rebase churn. Run B fully, then D on
   `services/`-module tests. D's persistence-test work (D1, D3 on DAO tests, D4) has
   no overlap with B and can start immediately.
2. **A and B both touch `MatchServiceImpl` / `TournamentServiceImpl`.** Not a logical
   conflict, but if two people work them simultaneously, agree on who merges first
   (recommended: A first — it's smaller and highest value).
3. Everything in C, E, F is conflict-free with A/B/D except trivial bundle-file
   merges (E1 vs other message-key additions).

### Suggested order / assignment

**Solo:** A → B → C → E → F → D (priority-weighted; D last because it's the largest
and partially blocked by B).

**Two people:**
- Dev 1: A → B → B4
- Dev 2: C + F + E (one short PR each) → D1–D4 (persistence-side first, services-side
  after B merges)

**Three people:** Dev 1: A → help D · Dev 2: B → B4 · Dev 3: C → E → F → D
(persistence side).

### Step details

**A1.** `TournamentService.findTournamentForHost(long, User)` and
`MatchService.findMatchForHost(long, User)` in `service-contracts` + impls.
Reuse `findByIdOrThrow` + `validateCanMutate` (tournament) and the equivalent match
ownership rule. Throws: `*NotFoundException` (missing id), `*ForbiddenActionException`
(not the host, admin/mod exempt per the elevated-role rule).
**A2.** Replace every `findPublicTournament(...).orElse(null)` /
`findMatchById(...).orElse(null)` in host-scoped handlers
(`HostTournamentController:160, 174, 292, 338, 380`; `HostController:133-150`) with
the new methods. This kills both the ownership hole and the NPE→500.
**A3.** `ModerationService.findReportForReporter(long, User)`; delete the inline
check at `UserModerationReportController:126`.
**A4.** Tests per AGENTS.md ("allowed and denied paths"): service tests for
owner-ok / non-owner-forbidden / admin-ok / missing-404; MockMvc tests asserting
403/404 view resolution on the GET edit routes.

**B1.** Build the mapping table from P1-4 (it is the inventory). Decide the two
conventions: null-guards (JDK-with-plain-message **or** `InvalidUserException` —
pick one) and pagination (`InvalidPaginationException` → 400 via
`GeneralExceptionHandler`).
**B2.** Replace each site; new exception classes follow the existing per-area
package style (`models/.../exceptions/<area>/`). Where a controller currently
catches `IllegalArgumentException` to build a message code, retarget the catch to
the domain type.
**B3 (optional).** `getMessageCode()` on `DomainException`; mechanical switch of the
~10 `"prefix." + getMessage()` call sites.
**B4.** Update service tests asserting exception types; add bundle keys for any new
codes (both files).

**C1.** `V16__reserved_noop.sql` containing only a comment (keeps applied DBs valid;
sequence becomes contiguous).
**C2.** Renumber `testmigration/` to mirror production numbers exactly; create no-op
test migrations for prod versions that are PostgreSQL-only. Verify with
`mvn test -pl persistence`.

**D1.** Fixed instants in `UserBanJpaDaoTest`, `TournamentMatchJpaDaoTest`,
`TournamentJpaDaoTest` (e.g. `Instant.parse("2026-04-05T00:00:00Z")` like the
service tests).
**D2.** Remove `Mockito.spy` from `MatchServiceImplTest:68-80`; for the
`assertDoesNotThrow`-only tests, assert the observable outcome (returned object,
persisted row via `em`, mail-recipient list on the fake) — never `verify()`.
**D3.** Convert the named copy-paste families to `@ParameterizedTest`
(`@EnumSource`/`@CsvSource`/`@MethodSource`).
**D4.** New tests: LIKE escaping (`%`/`_` in search input returns literal matches)
in `MatchJpaDaoTest` + `TournamentMatchJpaDaoTest`; targeted tests for each
`*DataServiceImpl` with query-building logic (start with `MatchDataServiceImpl`).

**E1–E5** and **F1–F5** are single-sitting mechanical edits; run
`mvn spotless:apply && mvn test` once per track before pushing.

### Verification gate (every track, before merge)

```bash
mvn spotless:apply && mvn test
```

Plus for Track A: manual check — log in as a non-owner, hit
`/host/matches/{id}/edit` and `/host/tournaments/{id}/edit` → expect 403; bogus id →
404 (was 500).

### Definition of done

- [ ] A: no host-scoped handler reaches a render path without an ownership-checked
      service lookup; nonexistent ids → 404; allow/deny tests in place
- [ ] B: zero `IllegalArgumentException`/`IllegalStateException` thrown as business
      outcomes in `services/` (null-guard convention documented); no hardcoded
      English exception messages
- [ ] C: contiguous V1–V37 in both migration trees; persistence tests green
- [ ] D: no `assertDoesNotThrow`-only tests; no `spy` on class under test; no
      `Instant.now()` in test setup/assertions; ≥3 test families parameterized;
      LIKE-escape tests exist
- [ ] E: no hardcoded visible copy in new dashboard code; no `"PAST"`/sport-name
      literals; zero `TODO` in `webapp/src/main/java`
- [ ] F: all 17 entities have id-based `equals`/`hashCode`; `messages_en.properties`
      deleted; `Image.contentLength` primitive; `DistanceUtils` final
