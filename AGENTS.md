# AGENTS.md

## 1. Purpose

This file guides AI coding agents working on this repository.

The repository is no longer restricted to the MVP. Work may now target the broader semester scope described in the PRD, but changes should still be incremental, grounded in the current codebase, and limited to the slice the user actually requested.

## 2. Sources of Truth

When requirements conflict, use this order:

1. The repository code and tests.
2. This `AGENTS.md`.
3. `./docs/prd.md`.
4. `./docs/design.md`.

If the code already establishes a pattern, follow it unless the task explicitly asks to replace it.

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
- JDBC via `JdbcTemplate` / `SimpleJdbcInsert`
- PostgreSQL at runtime
- HSQLDB + Spring Test for persistence tests
- JUnit 5 + Mockito for tests

Formatting is handled with Spotless from the root Maven build.

## 4. Product Scope

The repository is now allowed to implement the broader product scope, not only the MVP.

In scope when requested or required by the current phase:

- in-person sports matches/tournaments
- event discovery/feed flows
- event detail pages
- reserve a spot
- cancel a reservation
- public profiles
- host event creation
- host event editing
- host event deletion
- hosted-event management
- attendee visibility
- explicit user/host flow switching
- account registration, login, and logout
- role-based authorization
- reviews
- moderation / reporting

Still out of scope unless the user explicitly asks otherwise:

- Hibernate / JPA
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
- keep authentication and authorization logic centralized in Spring Security configuration, dedicated policy helpers, or services instead of scattering ownership rules through JSPs
- when changing access control, add or update tests for both allowed and denied paths

The existing mail-validation placeholder flow should be treated as legacy MVP infrastructure. Reuse or migrate it only when it helps a requested transition, not as a reason to avoid proper auth.

## 6. Architecture Rules

Respect the existing layered structure:

- `webapp`: controllers, request handling, view models, JSPs, navigation
- `services`: business rules and orchestration
- `service-contracts`: service interfaces
- `persistence`: JDBC DAOs and SQL-backed access
- `persistence-contracts`: DAO interfaces
- `models`: domain objects

Rules:

- controllers stay thin
- business rules go in services
- SQL and row mapping stay in `persistence`
- models stay simple and framework-light
- use Spring-managed beans and constructor injection
- do not instantiate services or DAOs with `new` inside managed classes

## 7. Persistence Rules

The current persistence path is JDBC, not Hibernate.

Rules:

- use DAOs in `persistence`
- use `JdbcTemplate`, `SimpleJdbcInsert`, and explicit SQL
- keep schema changes in `persistence/src/main/resources/schema.sql`
- when changing persistence behavior, update or add DAO tests

Do not introduce:

- JPA entities
- Hibernate mappings
- Spring Data repositories

Those may matter later, but they are not part of the current default implementation path.

## 8. Web And View Rules

This project is server-rendered.

Rules:

- use Spring MVC controllers and JSP views
- keep views under `webapp/src/main/webapp/WEB-INF/views`
- use JSTL / EL / `c:url`
- prefer reusable JSP tags for shared UI primitives
- reuse existing tags like `button.tag`, `card.tag`, and `textInput.tag` when appropriate
- keep CSS in the existing `webapp/src/main/webapp/css` structure

Do not introduce React, Next.js, Vue, Tailwind, Vite, or a client-rendered architecture.

The current `HelloWorldController` and demo pages are scaffolding. It is fine to replace or extend them toward real product flows, but preserve useful shared components unless the task says otherwise.

### Internationalization

This repository already ships with English and Spanish UI copy. Treat internationalization as an existing product requirement, not as optional polish.

Rules:

- do not hardcode new user-facing copy in controllers, services, JSPs, mail templates, or validation messages when that copy can come from the message bundles
- add every new user-facing string to both `webapp/src/main/resources/i18n/messages.properties` and `webapp/src/main/resources/i18n/messages_es.properties`
- keep controllers and services locale-aware when they prepare user-visible labels, verification messages, preview data, or generated links
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

## 11. Deferred Technology Notes

Spring Security is now an allowed part of the stack for authentication and authorization work.

The following technologies are still future concerns and should be kept in mind, but not implemented now unless the user explicitly asks for them:

- Hibernate / JPA
- Jersey / JAX-RS

Code written for the current product phase should not make those future additions harder, but it also should not partially implement them ahead of time.
