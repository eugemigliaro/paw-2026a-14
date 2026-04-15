# AGENTS.md

## 1. Purpose

This file guides AI coding agents working on this repository.

The goal is to implement the MVP only. Keep decisions tied to the current codebase and the first sprint scope, not the full semester roadmap.

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

## 4. MVP Scope

Build only what belongs to the MVP for this product.

In scope:

- in-person sports events
- event discovery/feed flows
- event detail pages
- reserve a spot
- cancel a reservation
- basic public profiles
- host event creation
- host event deletion
- hosted-event management needed for the MVP
- attendee visibility needed for the MVP
- explicit user/host flow switching if that flow is being built

Out of scope for the MVP unless the user explicitly asks otherwise:

- login/logout
- full authentication or authorization
- Spring Security
- Hibernate / JPA
- Jersey / JAX-RS
- reviews
- interactive maps
- personalized recommendations
- chat
- notifications
- waitlists
- moderation / reporting
- check-in / attendance validation
- platform payments
- host verification
- a SPA frontend or frontend framework migration

Important product rules:

- one person can act as both participant and host
- events are in-person only
- each event belongs to exactly one category
- the platform does not process payments
- search is free-text
- reservation cannot exceed capacity
- cancelling a reservation releases the spot
- a user can reserve their own event

If a requested change conflicts with this MVP scope, call it out before coding.

## 5. Authentication Workarounds For The MVP

Proper login is not part of this sprint.

The chosen MVP workaround is a simple mail-validation placeholder flow.

Rules:

- do not build real login/logout flows
- do not introduce Spring Security yet
- do not add role systems, permission frameworks, or password-auth flows
- when a feature needs identity, route it through the mail-validation placeholder flow
- keep temporary identity logic isolated behind a small helper, service, or clearly marked controller-level seam
- implement the workaround so it can be replaced cleanly in sprint 2

Meaning in practice:

- email can be used as the temporary identity anchor for MVP flows
- any verification step should stay simple and clearly marked as temporary
- do not spread ad hoc identity assumptions throughout controllers, services, and JSPs

Do not partially implement real authentication now. Keep the placeholder narrow and replaceable.

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

Those may matter later, but they are not part of the MVP implementation now.

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

The current `HelloWorldController` and demo pages are scaffolding. It is fine to replace or extend them toward real MVP flows, but preserve useful shared components unless the task says otherwise.

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

For MVP work:

- aim for clean, structured, calm UI
- prefer readable layouts over flashy interactions
- keep the premium sports/community tone
- use existing reusable components before inventing one-off markup

Avoid spending implementation time on design-system expansion that does not directly support MVP screens.

## 10. Testing Rules

Meaningful behavior changes should come with tests that match the current project patterns.

Use:

- JUnit 5 + Mockito for service tests
- Spring Test + HSQLDB for persistence tests

Expectations:

- add service tests when business rules change
- add DAO tests when queries or inserts change
- keep tests deterministic
- prefer one scenario per test
- do not use `Mockito.verify()` in unit tests
- structure every unit test as: 1. Arrange, 2. Exercise, 3. Assert

At minimum, run:

- `mvn test`

If formatting is needed, use the existing Spotless setup rather than ad hoc formatting.

## 11. Deferred Technology Notes

The following technologies are future concerns and should be kept in mind, but not implemented now unless the user explicitly asks for them:

- Spring Security
- Hibernate / JPA
- Jersey / JAX-RS

Code written for the MVP should not make those future additions harder, but it also should not partially implement them ahead of time.
