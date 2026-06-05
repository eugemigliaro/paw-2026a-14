# AGENTS.md

## 1. Purpose

This file guides AI coding agents working on this repository.

The repository is no longer restricted to the MVP. Work may now target the broader semester scope described in the PRD, but changes should still be incremental, grounded in the current codebase, and limited to the slice the user actually requested.

## 2. Sources of Truth

When product requirements or expected behavior conflict, use this order:

1. The current user request.
2. The repository code and tests.
3. This `AGENTS.md`.
4. `./docs/prd.md`.
5. `./docs/design.md`.

For implementation style, architecture, and agent behavior, this `AGENTS.md` is binding. Follow existing code patterns only when they do not conflict with this file; do not copy legacy patterns explicitly forbidden here.

## 3. Current Project Shape

This repository is currently a Maven multi-module Java project with these modules:

- `webapp`
- `services`
- `service-contracts`
- `persistence`
- `persistence-contracts`
- `models`

For a compact repository map focused on codebase orientation, see `./docs/repo-structure.md`.

The implemented stack today is:

- Java 21
- Spring MVC
- JSP + JSTL + EL
- custom JSP tags under `webapp/src/main/webapp/WEB-INF/tags`
- Spring dependency injection
- Hibernate / JPA for persistence (with Flyway for migrations)
- PostgreSQL at runtime
- HSQLDB + Spring Test for persistence tests
- JUnit 5 + Mockito for tests

Formatting is handled with Spotless from the root Maven build.

## 4. Product Scope

The repository is now allowed to implement the broader product scope, not only the MVP.

Still out of scope unless the user explicitly asks otherwise:

- Jersey / JAX-RS
- platform payments
- a SPA frontend or frontend framework migration

Important product rules:

- one person can act as both participant and host
- matches/tournaments are in-person only
- each event belongs to exactly one category
- the platform does not process payments
- search is free-text
- reservation cannot exceed capacity
- cancelling a reservation releases the spot
- a user can reserve their own event

Moving beyond the MVP does not mean implementing the whole roadmap at once. Build the smallest coherent slice that satisfies the current request, and call out product-rule conflicts before coding.

## 5. Authentication And Authorization

Real authentication and authorization are now allowed.

Rules:

- Spring Security is the default framework for login, logout, and access control work
- prefer server-rendered, session-backed authentication that fits the current Spring MVC + JSP stack unless the task explicitly asks for something else
- use email/password as the baseline identity flow unless the user explicitly requests additional providers
- manual user/host mode switching remains a product UX concept; it is not a substitute for security roles
- support two authorization levels unless the task explicitly asks for a different model:
    - regular authenticated user
    - elevated admin/mod role with near-full platform access
- the elevated admin/mod role can intervene across ownership boundaries, including editing or deleting any event and handling moderation/reporting flows
- regular users should only manage their own profile, reservations, and hosted content unless an explicit business rule expands access
- keep authentication and authorization logic centralized in Spring Security configuration or dedicated security/policy helpers; services may defensively enforce domain invariants, but controllers and JSPs must not own access rules
- when changing access control, add or update tests for both allowed and denied paths

The existing mail-validation placeholder flow should be treated as legacy MVP infrastructure. Reuse or migrate it only when it helps a requested transition, not as a reason to avoid proper auth.

## 6. Architecture Rules

Respect the existing layered structure:

- `webapp`: controllers, request binding, JSPs, tags, navigation, and minimal view data
- `services`: business rules and orchestration
- `service-contracts`: service interfaces
- `persistence`: JPA repositories and Hibernate entity mappings
- `persistence-contracts`: repository/DAO interfaces
- `models`: JPA entities and domain objects

Rules:

- controllers stay thin
- business rules go in services
- database access, query logic, and entity mapping stay in `persistence`
- models stay simple and framework-light
- use Spring-managed beans and constructor injection
- do not instantiate services or DAOs with `new` inside managed classes

## 7. Persistence Rules

The production persistence layer uses Hibernate / JPA with Flyway for migrations.

The final hand-in should have no production JDBC. JDBC, `JdbcTemplate`, `RowMapper`, and direct SQL access are acceptable only in tests or Flyway migration scripts.

Rules:

- use JPA repositories in `persistence`
- define JPA entities in `models` or `persistence` as appropriate; keep entity classes clean and framework-light
- use Hibernate annotations (`@Entity`, `@Table`, `@Column`, `@OneToMany`, `@ManyToOne`, etc.) for mapping
- keep database migrations in `persistence/src/main/resources/db/migration` managed by Flyway
- transaction management is handled by Spring's `@Transactional`
- when changing persistence behavior, update or add repository tests
- prefer explicit DAO methods backed by JPQL, criteria, named queries, or repository helpers already present in the project; do not add Spring Data just to solve a DAO
- treat existing production JDBC code as legacy migration work; when touching it, migrate the touched behavior to Hibernate / JPA instead of extending the JDBC implementation

Do not introduce:

- JDBC, `JdbcTemplate`, `RowMapper`, or production native-SQL persistence code
- Hibernate session management outside of Spring's declarative transaction handling
- direct SQL in application code outside of migration scripts

## 8. Web And View Rules

This project is server-rendered.

Rules:

- use Spring MVC controllers and JSP views
- keep views under `webapp/src/main/webapp/WEB-INF/views`
- use JSTL / EL / `c:url`
- do not use `${pageContext.request.contextPath}` directly in JSP, JSPF, or tag files; use `<c:url>` for context-relative URLs
- prefer reusable JSP tags for shared UI primitives
- reuse existing tags like `button.tag`, `card.tag`, and `textInput.tag` when appropriate
- keep CSS in the existing `webapp/src/main/webapp/css` structure

Do not introduce React, Next.js, Vue, Tailwind, Vite, or a client-rendered architecture.

Replace or extend scaffolding and demo-only pages toward real product flows when the requested work touches them, but preserve useful shared components unless the task says otherwise.

### Internationalization

This repository already ships with English and Spanish UI copy. Treat internationalization as an existing product requirement, not as optional polish.

Rules:

- do not hardcode new user-facing copy in controllers, services, JSPs, mail templates, or validation messages when that copy can come from the message bundles
- add every new user-facing string to both `webapp/src/main/resources/i18n/messages.properties` and `webapp/src/main/resources/i18n/messages_es.properties`
- prefer passing message codes, domain values, and formatting inputs to JSPs; resolve messages in Java only for non-JSP surfaces such as emails, flash messages, validation errors, metadata, or generated links, and always use the active/recipient locale
- do not use fixed locales such as `Locale.US` for user-facing date/time formatting; use the active request locale instead
- preserve locale across flows that leave the current page, especially verification links, redirects, and mail-driven entry points
- keep document metadata locale-aware, including `<html lang>` and relevant accessibility labels
- when changing a localized flow, add or update tests that exercise at least one non-default locale path, not only English

If a change adds visible UI or email copy and only updates one locale, call that out as incomplete before coding.

## 9. Design Guidance

Use `./docs/design.md` as direction, not as permission to overbuild the frontend.

For product work:

- aim for clean, structured, calm UI
- prefer readable layouts over flashy interactions
- keep the premium sports/community tone
- use existing reusable components before inventing one-off markup

Avoid spending implementation time on design-system expansion that does not directly support the requested screens.

## 10. Testing Rules

Meaningful behavior changes should come with tests that match the current project patterns.

Use:

- JUnit 5 + Mockito for service tests
- Spring Test + HSQLDB for persistence tests

Expectations:

- add service tests when business rules change
- add DAO tests when queries or inserts change
- add controller or integration tests when login, role checks, or protected-route behavior changes
- keep tests deterministic
- prefer one scenario per test
- do not use `Mockito.verify()` in unit tests
- structure every unit test as: 1. Arrange, 2. Exercise, 3. Assert

At minimum, run:

- `mvn test`

If formatting is needed, use the existing Spotless setup rather than ad hoc formatting.

## 11. Logging Rules

Logging standards for this repository are defined in `./docs/logging.md`.

When changing or adding behavior in `webapp`, `services`, or `persistence`:

- follow `SLF4J + Logback` conventions from `docs/logging.md`
- avoid overlogging and redundant logs across layers
- never log sensitive information (passwords, tokens, raw secrets, session identifiers, full personal data)
- prefer stable identifiers and business outcomes (`matchId`, `userId`, status transitions)
- keep test logging console-based via `logback-test.xml`

## 12. Do Not Repeat TP1 Mistakes

The TP1 correction PDF exposed repeated class-wide failure patterns. This section is not a fix backlog; it constrains future work. Do not add new violations, and when touching nearby legacy code, clean up the violation if the cleanup is small and safe.

### Before Writing Code

- first check whether Spring MVC, Spring Security, JSP/JSTL, Bean Validation, JPA, or the existing project helpers already solve the problem
- do not introduce custom infrastructure, component factories, filters, parsers, session synchronizers, or DTO layers unless they remove real complexity
- do not expose half-built future states or UI controls for functionality that is not implemented end-to-end
- keep `AGENTS.md` as the project workflow source; do not create overlapping agent context files with divergent rules
- only add code you can explain in terms of the product behavior it supports

### Build And Repository Hygiene

- never commit generated artifacts, IDE settings, scraped datasets, `target/` directories, or `webapp.war`
- centralize dependency and plugin versions in the parent POM; child POMs should inherit versions and Java release settings
- use correct dependency scopes in the parent, especially `provided` for container APIs and `test` for test libraries
- do not put dependencies in the parent `<dependencies>` block unless every child module truly needs them
- preserve module boundaries: upper layers depend on contracts, not implementation modules; `models` must not gain web/serialization concerns casually
- keep production and test migrations aligned; no version gaps, no startup-time manual migrations, no tables without a DAO/service owner

### Controllers And Layering

- controllers bind request data, call one service/facade operation, choose the view or redirect, and map binding errors
- controllers must not contain business rules, multi-step orchestration, ownership checks, role checks, visibility checks, filtering, pagination, or binary/file processing logic
- controller support classes count as controller code; do not hide business logic in `MvcSupport`, view factories, or helpers
- `webapp` code, including security config and filters, must not inject DAOs or depend on persistence implementations
- services own business rules, transactions, and cross-aggregate orchestration; services do not return view names or redirect destinations
- a service should use its own aggregate DAO and call other services for other aggregates instead of injecting many unrelated DAOs
- write flows need `@Transactional`; read-only service methods need `@Transactional(readOnly = true)`

### Security

- prefer one access-control convention and keep it centralized; for web routes, default to `SecurityConfig` rather than scattered controller checks
- do not manually redirect to login, inspect roles in controllers, or use reflection/stringly typed access to the principal
- use typed Spring Security principals, `@AuthenticationPrincipal`, or shared model advice for authenticated-user exposure
- externalize secrets and security keys, especially remember-me keys; never provide a hardcoded fallback
- derive app base URLs for emails from configuration, not from request host headers
- use Spring Security static-resource ignoring instead of duplicating permit rules or custom static handling
- after authentication, verification, ban, or role changes, ensure the current session/principal state and error pages render correctly

### Forms, Validation, And URLs

- put input rules in form objects, Bean Validation annotations, and custom validators, not controller `if` blocks
- let Spring bind request parameters to typed values such as `Long`, enums, dates, and booleans; avoid manual trim/parse/normalize code in controllers
- invalid pagination, page size, filter, enum, or date input must produce a controlled validation error or 400, not a 500
- never detect validation outcomes by comparing hardcoded exception message strings
- build filter/search/pagination URLs with normal GET forms or `<c:url>`/`<c:param>` so query values are encoded and the context path is preserved
- escape user-controlled output in JSPs with JSTL/JSP mechanisms; do not inject returned HTML/JS into the DOM unless it is deliberately sanitized

### Persistence And Query Design

- filtering, searching, sorting, deduplication, counting, pagination, `offset`, and `limit` belong in DAO/database queries, not in Java collections
- do not load `findAll()`, `Integer.MAX_VALUE`, or arbitrary huge pages to filter or paginate in memory
- avoid N+1 access: do not fetch IDs and loop over `findById`; add DAO methods that return the needed entity/projection set in one query
- avoid Java joins; use database joins, `WHERE id IN (...)`, projections, or fetch plans as appropriate
- escape SQL/JPQL `LIKE` wildcards (`%`, `_`) in text searches
- write explicit count queries or shared query builders; do not derive counts with fragile string replacement
- each DAO owns its entity/table area; do not update unrelated tables from the wrong DAO
- do not add or extend production `RowMapper`/`JdbcTemplate` code; migrate touched legacy JDBC paths to Hibernate / JPA

### Internationalization, Time, And Mail

- all user-visible copy, including validation errors, ban reasons, aria labels, titles, and mail text, must live in both message bundles
- prefer JSP `<spring:message>` for ordinary view labels instead of resolving labels in controllers and passing strings to JSPs
- preserve locale through redirects, verification links, emails, and async work; do not rely on request-thread locale for recipient-specific async emails
- mail services should own standardized business emails and templates; callers should request a specific email action, not assemble arbitrary `MailContent`
- emails that require user action need clear CTAs back into the app with context-correct URLs
- date/time handling must preserve the intended wall-clock time; use explicit zones and test locale/time-zone sensitive flows

### Views And View Models

- keep JSP rendering in JSPs; do not split view composition across JSPs, controllers, and Java component factories without a concrete need
- prefer domain models or focused form/view data that JSP EL can consume directly; avoid DTOs that just copy fields and get unpacked again
- use existing JSP tags and includes for repeated UI; do not create Java-side shell abstractions for what Spring Security taglibs or JSPs can render
- do not render maps or location widgets when the event has no usable coordinates/location data
- check long labels, recurrence options, buttons, cards, and forms at realistic viewport widths so text does not clip or overflow

### Tests

- tests must validate behavior, not implementation details
- do not use `Mockito.verify()`, `doAnswer`, `AtomicReference`, `AtomicBoolean`, argument-captor clones, or interaction-order assertions in service unit tests
- do not write tests that only assert `assertDoesNotThrow()` or that a JSP contains a literal text fragment
- keep one behavior scenario per test; use parameterized tests for enum/value matrices
- persistence tests should set up and assert state independently of the DAO method being tested
- controller/route tests should use mocks, not giant custom service implementations
- add targeted tests for negative pages, malformed filters, wildcard search escaping, authorization denial, XSS-sensitive rendering, locale, mail links, and time-zone behavior when touching those areas
