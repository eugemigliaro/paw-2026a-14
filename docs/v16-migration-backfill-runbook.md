# V16 Migration Backfill Runbook

## Purpose

Production databases already have `V17` and later migrations, but the repository
did not contain a `V16`. The repository now contains an intentional no-op migration:

`V16__reserve_migration_version.sql`

The PostgreSQL migration executes `SELECT 1`; its HSQLDB counterpart executes
`VALUES (1)`. Neither changes application schema or data. Existing databases must
apply the PostgreSQL migration once with Flyway's out-of-order mode. Fresh databases
apply it normally between `V15` and `V17`.

## Safety Rules

- Do not renumber or edit `V17` or later migrations.
- Do not insert a row into `flyway_schema_history` manually.
- Do not use `flyway repair` to manufacture the missing entry.
- Do not put schema or data changes in `V16`.
- Enable out-of-order mode only for the controlled backfill deployment.
- Perform the procedure independently on every persistent environment.

## Before Deployment

1. Back up the target PostgreSQL database using the environment's normal backup
   procedure.
2. Confirm that no `V16` entry already exists:

```sql
SELECT installed_rank, version, description, type, checksum, success
FROM flyway_schema_history
WHERE version = '16';
```

The expected result before the backfill is zero rows.

3. Confirm that later versions are already present and successful:

```sql
SELECT installed_rank, version, description, type, checksum, success
FROM flyway_schema_history
WHERE version >= '17'
ORDER BY installed_rank;
```

4. Build and test the exact revision that will be deployed:

```sh
mvn test
mvn spotless:check
```

## Backfill Deployment

The application reads the Spring property `flyway.outOfOrder`, which defaults to
`false`. Supply it as a JVM system property for one startup:

```text
-Dflyway.outOfOrder=true
```

Examples:

- Local Jetty verification:

  ```sh
  mvn -pl webapp -Dflyway.outOfOrder=true jetty:run
  ```

- External servlet container: add `-Dflyway.outOfOrder=true` to that deployment's
  JVM options, deploy the WAR, and allow the application to start once.

At startup, Flyway takes its normal database lock, discovers `V16`, executes
`SELECT 1`, and records the migration after the already installed later versions.

Do not run multiple manually managed Flyway processes against the same environment.
Normal clustered application startup is protected by Flyway's database lock, but a
single controlled instance is easier to observe for this one-time operation.

## Verify The Backfill

After the application starts successfully, run:

```sql
SELECT installed_rank, version, description, type, checksum, success
FROM flyway_schema_history
WHERE version = '16';
```

Expected values:

- `version`: `16`
- `description`: `reserve migration version`
- `success`: `true`
- `installed_rank`: greater than the rank of the migrations that were already
  deployed

Then inspect the full recent history:

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Application startup and Flyway validation must complete without an error.

## Return To Normal Configuration

1. Remove `-Dflyway.outOfOrder=true` from the JVM options.
2. Restart or redeploy the same application revision.
3. Confirm that startup succeeds with the default `flyway.outOfOrder=false`.
4. Run the `V16` verification query again and confirm there is exactly one successful
   row.

Leaving out-of-order mode enabled is not part of the solution. The permanent state
is the committed `V16` migration plus a successful history entry in every database.

## Fresh Database Verification

A clean database must apply migrations in version order without enabling
out-of-order mode. Verify this through the normal test suite and, when available,
the PostgreSQL migration integration profile described in
`docs/database-migration-parity-plan.md`.

The expected order around the repaired gap is:

```text
V15 -> V16 -> V17 -> V18 -> ...
```

## Failure Handling

`V16` contains only `SELECT 1`, so it should not alter schema or data if startup
fails elsewhere.

If Flyway reports an error:

1. Stop the deployment.
2. Do not edit `flyway_schema_history` and do not run `repair` automatically.
3. Capture the Flyway error and query the `V16` history row.
4. Confirm that the deployed WAR contains exactly
   `V16__reserve_migration_version.sql` with the expected checksum.
5. Resolve configuration or migration-history inconsistencies before retrying.

If `V16` is successful but a later application component fails, keep the successful
history row. The no-op migration has already completed correctly.

## Completion Checklist

- [ ] `V16` exists in both production and HSQLDB migration directories.
- [ ] The automated tests cover fresh and out-of-order application.
- [ ] Every persistent environment has one successful `V16` history row.
- [ ] Out-of-order mode has been removed after the backfill.
- [ ] The application restarts successfully with out-of-order mode disabled.
- [ ] Fresh database migration and the full test suite pass.
