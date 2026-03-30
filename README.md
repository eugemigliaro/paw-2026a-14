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
