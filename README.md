## Setup and Instalation Instructions

After cloning the repository, run:

```sh
sh scripts/setup-git-hooks.sh
sh scripts/setup-remotes.sh
```

### Database migrations (Flyway)

Database schema is managed with Flyway.

- Main migrations: `persistence/src/main/resources/db/migration`
- Test migrations: `persistence/src/test/resources/db/testmigration`

Flyway automatically runs migrations on application startup and during persistence tests.

### Runtime configuration

This project uses a committed Spring `application.properties` file whose values are filled by Maven resource filtering from gitignored profile files.

Local development uses:

- `config/local.properties`

Pampero deployment packaging uses:

- `config/pampero.properties`

The repository includes example files for both environments:

- `config/local.example.properties`
- `config/pampero.example.properties`

Do not commit the real `config/local.properties` or `config/pampero.properties` files.

### Logging

This repository uses **SLF4J + Logback** for application logging.

- Runtime Logback config: `webapp/src/main/resources/logback.xml`
- Test Logback config: `webapp/src/test/resources/logback-test.xml`
- Logging rules and examples: `docs/logging.md`

### Run locally

Create `config/local.properties` from `config/local.example.properties`, then install the current
multi-module snapshots and run the webapp:

```sh
mvn install -DskipTests
cd webapp
mvn jetty:run
```

`mvn jetty:run` uses the default `local` Maven profile, so no extra `source` or environment export is needed.
Run `mvn install -DskipTests` again after changing classes in `models`, `service-contracts`,
`persistence-contracts`, `persistence`, or `services`; otherwise Jetty may load stale
`1.0-SNAPSHOT` jars from the local Maven repository.

### Build For Pampero

Create `config/pampero.properties` with the faculty DB credentials, then package the WAR with:

```sh
mvn -Ppampero clean package
```

The generated WAR is:

```sh
webapp/target/webapp.war
```

Upload that file as `web/app.war` through the faculty SFTP flow.

## Daily Workflow

## Branch Policy

- `main` contains deployed content.
- `dev` contains the sprint work in progress.
- Do not push directly to `main`; use branches and Pull Requests.

1. Create a branch:

```sh
git checkout -b feature/my-feature
```

2. Work and commit:

```sh
git add .
git commit -m "feat(module): short description"
```

3. Push your branch:

```sh
git push origin my-branch
```

4. Open a Pull Request → **do not push directly to `main`**

---

## Delivery (Bitbucket)

When submitting:

```sh
git push bitbucket --all
git push bitbucket --tags
```

---

## If something fails

- Formatting:

```sh
mvn spotless:apply
```

- Hooks not running:

```sh
git config --get core.hooksPath
```

(should return `.githooks`)
