# PAW Grupo 14

Runbook for local development and packaging.

## Requirements

- Java 21
- Maven
- PostgreSQL

## First-Time Setup

After cloning the repository, install the project git hooks:

```sh
sh scripts/setup-git-hooks.sh
```

If you need the faculty/Bitbucket remote configured too, run:

```sh
sh scripts/setup-remotes.sh
```

## Local Configuration

Create the local config file from the example:

```sh
cp config/local.example.properties config/local.properties
```

Edit `config/local.properties` with your local PostgreSQL credentials.

Do not commit real local or deployment credentials. The real files are gitignored:

- `config/local.properties`
- `config/pampero.properties`

## Database

Database schema is managed by Flyway. Migrations run automatically on application startup and during persistence tests.

For local development, make sure the database configured in `config/local.properties` exists before starting Jetty.

## Run Locally

Install the current multi-module snapshots and start the webapp:

```sh
mvn clean install
cd webapp
mvn jetty:run
```

`mvn jetty:run` uses the default `local` Maven profile.

After changing code in `models`, `service-contracts`, `persistence-contracts`, `persistence`, or `services`, run the install step again so Jetty does not load stale `1.0-SNAPSHOT` jars:

```sh
mvn install -DskipTests
```

## Tests and Formatting

Run the test suite:

```sh
mvn test
```

Apply formatting:

```sh
mvn spotless:apply
```

Check formatting without changing files:

```sh
mvn spotless:check
```

## Build WAR

Create `config/pampero.properties` with the faculty DB credentials, then package the deployable WAR:

```sh
mvn -Ppampero clean package
```

The generated WAR is:

```sh
webapp/target/webapp.war
```

## Users

| User    | email                          | password           |
| ------- | ------------------------------ | ------------------ |
| Admin   | matchpointpawapp@gmail.com<br> | _matchPointAdmin14 |
| Regular | matchpointuser@gmail.com       | 12345678           |

## More Docs

- Repository structure: `docs/repo-structure.md`
- Product requirements: `docs/prd.md`
- Design direction: `docs/design.md`
- Logging rules: `docs/logging.md`
