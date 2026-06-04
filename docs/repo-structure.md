# Repo Structure

This file is a quick orientation map for the codebase. It complements `AGENTS.md`
instead of repeating its rules.

## Overview

The repository is a Maven multi-module Java 21 application built around:

- Spring MVC
- JSP + JSTL + EL
- Spring dependency injection
- Hibernate / JPA
- PostgreSQL at runtime
- Flyway migrations
- HSQLDB for persistence tests

The main deployable module is `webapp`, which packages the WAR.

## Modules

- `webapp`: controllers, form objects, view models, JSP views, JSP tags, static
  assets, Spring MVC configuration, and `WEB-INF/web.xml`.
- `services`: business logic, transactions, orchestration, mail flows, and
  product rules.
- `service-contracts`: service interfaces plus service-layer request and result
  classes.
- `persistence`: JPA DAO implementations, Hibernate mappings where applicable,
  Flyway migrations, and persistence integration tests.
- `persistence-contracts`: DAO interfaces used by the service layer.
- `models`: shared domain classes, JPA entities, and enums such as `Match`,
  `Tournament`, `User`, `Sport`, and `PaginatedResult`.

## Dependency Direction

Read the app top-down like this:

`webapp -> services -> persistence -> database`

Supporting contract modules sit between layers:

- `webapp` depends on service contracts.
- `services` depends on persistence contracts.
- `models` is shared by the contract and implementation layers.

## Important Paths

- Spring web configuration: `webapp/src/main/java/ar/edu/itba/paw/webapp/config`
- Controllers: `webapp/src/main/java/ar/edu/itba/paw/webapp/controller`
- Web forms: `webapp/src/main/java/ar/edu/itba/paw/webapp/form`
- UI view models: `webapp/src/main/java/ar/edu/itba/paw/webapp/viewmodel`
- JSP views: `webapp/src/main/webapp/WEB-INF/views`
- Reusable JSP tags: `webapp/src/main/webapp/WEB-INF/tags`
- Static assets: `webapp/src/main/webapp/css`, `webapp/src/main/webapp/js`,
  and `webapp/src/main/webapp/assets`
- Service implementations: `services/src/main/java/ar/edu/itba/paw/services`
- DAO implementations: `persistence/src/main/java/ar/edu/itba/paw/persistence`
- Flyway migrations: `persistence/src/main/resources/db/migration`
- Runtime property template: `webapp/src/main/resources/application.properties`
- Environment-specific filtered values: `config/local.properties` and
  `config/pampero.properties`

## Tests

- `services/src/test/java`: service tests with JUnit 5 and Mockito.
- `persistence/src/test/java`: persistence integration tests with Spring Test and
  HSQLDB.
- `webapp/src/test/java`: controller and route tests with `MockMvc`.

## Ignore By Default

Unless the task points to them, these are usually not the first places to inspect:

- module `target/` directories
- legacy/demo `helloworld` views
- schema areas not wired into current product flows
