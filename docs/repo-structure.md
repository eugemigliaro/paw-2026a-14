# Repo Structure

This file is a quick orientation map for the codebase. It complements `AGENTS.md` instead of repeating its rules.

## Overview

The repository is a Maven multi-module Java 21 application built around:

- Spring MVC
- JSP + JSTL + EL
- Spring dependency injection
- JDBC with `JdbcTemplate` / `SimpleJdbcInsert`
- PostgreSQL at runtime
- Flyway migrations
- HSQLDB for persistence tests

The main deployable module is `webapp`, which packages the WAR.

## Modules

- `webapp`
  - Web entry point.
  - Contains Spring MVC config, controllers, form objects, view models, JSP views, JSP tags, static CSS/JS/assets, and `WEB-INF/web.xml`.

- `services`
  - Business logic and orchestration.
  - Contains the active MVP behavior: event search, reservation rules, email verification flows, placeholder identity resolution by email, image validation/storage orchestration, and mail configuration/templates.

- `service-contracts`
  - Service interfaces plus service-layer request/result classes.

- `persistence`
  - JDBC DAO implementations and Flyway migrations.
  - Also contains persistence integration tests.

- `persistence-contracts`
  - DAO interfaces used by the service layer.

- `models`
  - Shared domain/data classes and enums such as `Match`, `User`, `Sport`, `PaginatedResult`, and verification payload/status types.

## Dependency Direction

Read the app top-down like this:

`webapp -> services -> persistence -> database`

Supporting contract modules sit between layers:

- `webapp` depends on service contracts
- `services` depends on persistence contracts
- `models` is shared by both contract layers

## Important Paths

- Spring web configuration:
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/config/WebConfig.java`

- Servlet bootstrap:
  - `webapp/src/main/webapp/WEB-INF/web.xml`

- Controllers:
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/controller`

- Web forms:
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/form`

- UI view models:
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/viewmodel`

- JSP views:
  - `webapp/src/main/webapp/WEB-INF/views`

- Reusable JSP tags:
  - `webapp/src/main/webapp/WEB-INF/tags`

- Static assets:
  - `webapp/src/main/webapp/css`
  - `webapp/src/main/webapp/js`
  - `webapp/src/main/webapp/assets`

- Service implementations:
  - `services/src/main/java/ar/edu/itba/paw/services`

- Mail-related services and templates:
  - `services/src/main/java/ar/edu/itba/paw/services/mail`
  - `services/src/main/resources/mail`

- DAO implementations:
  - `persistence/src/main/java/ar/edu/itba/paw/persistence`

- Flyway migrations:
  - `persistence/src/main/resources/db/migration`

- Runtime property template:
  - `webapp/src/main/resources/application.properties`

- Environment-specific filtered values:
  - `config/local.properties`
  - `config/pampero.properties`

## Main Runtime Flows

- Feed / discovery:
  - `GET /` -> `FeedController` -> `MatchService` -> `MatchJdbcDao` -> `WEB-INF/views/feed/index.jsp`

- Event detail:
  - `GET /matches/{id}` -> `EventController` -> `MatchService` / `UserService` -> `WEB-INF/views/matches/detail.jsp`

- Reservation request:
  - `POST /matches/{id}/reservations` -> `EventController` -> `ActionVerificationService`

- Host event creation:
  - `GET|POST /host/matches/new` -> `HostController` -> `ActionVerificationService`

- Email confirmation:
  - `GET|POST /verifications/{token}` -> `VerificationController` -> `ActionVerificationServiceImpl`

- Image serving:
  - `GET /images/{id}` -> `ImageController` -> `ImageService` -> `ImageJdbcDao`

## Current MVP Surface

The code that is most active today is centered on:

- public event feed/discovery
- event detail pages
- reserve-a-spot flow
- host event creation
- one-time email verification
- attendee visibility on event detail pages
- optional banner image upload and delivery

The schema is broader than the currently wired app surface, so do not assume every table already has service/controller support.

## Tests

- `services/src/test/java`
  - unit tests with JUnit 5 + Mockito

- `persistence/src/test/java`
  - JDBC integration tests with Spring Test + HSQLDB

- `webapp/src/test/java`
  - route/controller tests with `MockMvc`

## Ignore By Default

Unless the task points to them, these are usually not the first places to inspect:

- module `target/` directories
- legacy/demo `helloworld` views
- schema areas not wired into the current MVP flows
