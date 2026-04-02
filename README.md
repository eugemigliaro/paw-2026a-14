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

### Run locally

Create `config/local.properties` from `config/local.example.properties`, then run:

```sh
cd webapp
mvn jetty:run
```

`mvn jetty:run` uses the default `local` Maven profile, so no extra `source` or environment export is needed.

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
