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

The deployed WAR expects these runtime variables to be available to the Tomcat process:

- `PAW_DB_URL`
- `PAW_DB_USERNAME`
- `PAW_DB_PASSWORD`

For local development, the repository includes:

- `.env.example` with the required variable names
- `.env` with the current local values

Before running Jetty or deploying to Tomcat, make sure those variables are exported into the process environment. The application now fails fast at startup if any of them are missing.

## Daily Workflow

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
