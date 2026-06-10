# Database Migration Parity Plan

## Purpose

This plan resolves the correction that the test database scripts do not correspond
to the production migrations. The goal is not to make PostgreSQL and HSQLDB SQL
text identical. The goal is to make both migration histories describe the same
schema changes, in the same order, with equivalent database behavior.

The production migration directory is:

`persistence/src/main/resources/db/migration`

The HSQLDB test migration directory is:

`persistence/src/test/resources/db/testmigration`

## Current Findings

The correction is valid. Tests explicitly configure Flyway to use only
`classpath:db/testmigration`, so production migrations are not exercised by the
normal persistence test suite.

The two histories currently diverge in several ways:

- Production has versions through `V37`; tests have a separately numbered history
  through `V35`.
- The same feature appears under different versions. For example, recurring match
  series is production `V17` but test `V11`, and moderation support is production
  `V19` but test `V13`.
- Some test migrations contain changes that production introduces in several later
  migrations. Test `V8`, for example, combines multiple join-policy and participant
  status changes.
- Some production migrations have no test counterpart, particularly the sequence
  alignment and enum-to-varchar migrations from `V23` through `V29`.
- Test `V32` creates `pairing_strategy`, although production creates the tournament
  tables in `V33` and adds `pairing_strategy` separately in `V34`.
- There is behavioral drift. One known example is that production initially checks
  that `matches.price_per_player >= 0`, while the HSQLDB test schema does not enforce
  that check.
- The repository currently has no checked-in CI configuration, so PostgreSQL
  migration verification is not automatically enforced before merge.

Git history also shows that migration versions were renamed to match an already
deployed database state. In particular, production `V16__add_player_reviews.sql`
was renamed to `V18__add_player_reviews.sql` in commit `095803c4`. Therefore, the
gap is closed with the intentional no-op `V16__reserve_migration_version.sql`, not
by renumbering existing production files. Existing databases apply it once with
out-of-order mode according to `docs/v16-migration-backfill-runbook.md`.

## Non-Negotiable Rules

1. Existing production migrations that may have been applied are immutable. Do not
   rename, delete, reorder, or edit `V1` through `V37` after their deployed checksums
   have been confirmed.
2. The deployed PostgreSQL `flyway_schema_history` table is the authority for the
   production version ledger. Git history is supporting evidence, not a substitute
   for checking the deployed ledger.
3. The HSQLDB database is disposable. Its migration files may be replaced and
   renumbered to match production.
4. Every production version, including the reserved `V16`, must have one test
   counterpart with the same version
   and description, including PostgreSQL-specific migrations that become documented
   no-ops on HSQLDB.
5. A test migration must represent only the change made by its production
   counterpart. Later schema state must not be folded into earlier test migrations.
6. Dialect differences are permitted; missing constraints, defaults, relationships,
   or business behavior are not.
7. Any newly discovered production schema defect must be corrected with a new
   forward migration, starting with `V38`, and with a matching test migration.

## Phase 1: Establish The Canonical Production Ledger

Before changing migration files, capture the state of every PostgreSQL environment
that matters, including the shared development or deployed database.

1. Export Flyway information, including version, description, type, checksum, and
   success state, from `flyway_schema_history`.
2. Compare the exported versions and checksums with the current production files.
3. Confirm that `V16` is absent before performing the controlled backfill, or confirm
   that an existing `V16` entry matches the committed reserved migration.
4. Record the result in the implementation pull request.
5. Stop and resolve any checksum mismatch before editing migration infrastructure.

The current repository decision is to close the gap with a no-op `V16`. Existing
databases that already contain later versions apply it once with
`flyway.outOfOrder=true`, then return to the default `false` setting. If a database
already contains a different `V16`, stop and reconcile that environment before
deployment.

## Phase 2: Define The Version Mapping

Create a migration ledger used during implementation. It should have one row per
production version and contain:

- production filename;
- replacement HSQLDB filename;
- affected tables, columns, constraints, indexes, and sequences;
- required HSQLDB syntax adaptation;
- expected behavior after the migration;
- verification test, when the migration introduces a database rule.

The important current remapping is:

| Production history | Current test implementation | Required action |
| --- | --- | --- |
| `V1`-`V7` | `V1`-`V7` | Keep versions, remove prematurely included later state |
| `V8` | Folded into test `V1` | Create test `V8` for pending approval status |
| `V9` | Test `V9` | Keep as test `V9` |
| `V10` | Folded into test `V2` | Create test `V10` for the `other` sport |
| `V11`-`V15` | Mostly combined in test `V8` and earlier files | Split into test `V11`-`V15` |
| `V16` | Absent | Add the matching reserved no-op migration and backfill it once |
| `V17` | Test `V11` | Replace with test `V17` |
| `V18` | Test `V12` | Replace with test `V18` |
| `V19` | Test `V13` | Replace with test `V19` |
| `V20` | Test `V14` | Replace with test `V20` |
| `V21` | Test `V15` | Replace with test `V21` |
| `V22` | Test `V22` | Keep as test `V22` |
| `V23`-`V29` | Mostly absent | Add same-version HSQLDB migrations or documented no-ops |
| `V30` | Test `V30` | Keep as test `V30` after validating equivalence |
| `V31` | Absent | Add test `V31` for player-review sequence alignment |
| `V32` | Test `V31` | Replace with test `V32` |
| `V33` | Test `V32` | Replace with test `V33` without pairing strategy |
| `V34` | Folded into test `V32` | Create separate test `V34` |
| `V35` | Test `V33` | Replace with test `V35` |
| `V36` | Test `V34` | Replace with test `V36` |
| `V37` | Test `V35` | Replace with test `V37` |

## Phase 3: Rebuild The HSQLDB Migration History

Replace the current test migration set in one focused change. Do not incrementally
rename files while leaving a temporarily mixed history on the branch.

For every production migration:

1. Create an HSQLDB file with exactly the same version and description.
2. Translate PostgreSQL syntax while preserving the migration's responsibility.
3. Preserve table and column names, nullability, defaults, numeric precision,
   foreign keys, delete actions, uniqueness, check constraints, and index intent.
4. Keep initial test migrations at their historical state. Add later enum values,
   columns, and constraints only in the corresponding later migration.
5. Add a short SQL comment when an operation must differ for HSQLDB.
6. Use a comment-only migration only when no meaningful HSQLDB operation exists.
   State which PostgreSQL behavior is being treated as a no-op and why.

Expected dialect translations include:

- `BIGSERIAL` to an explicit HSQLDB sequence and a `BIGINT` default;
- `TIMESTAMPTZ` to the timestamp representation used by the HSQLDB tests;
- PostgreSQL enum types to `VARCHAR` columns with equivalent `CHECK` constraints;
- `NOW()` to `CURRENT_TIMESTAMP`;
- PostgreSQL sequence ownership or alignment to the closest HSQLDB sequence
  operation, or a documented no-op if it has no HSQLDB equivalent;
- PostgreSQL partial unique indexes to an HSQLDB constraint or index with verified
  equivalent null behavior.

The replacement is complete only when Flyway can migrate a fresh HSQLDB database
through the entire canonical version set and Hibernate's existing
`hbm2ddl.auto=validate` check succeeds.

## Phase 4: Audit Behavioral Schema Parity

File-name parity is necessary but insufficient. Compare the final PostgreSQL and
HSQLDB schemas using a checklist for every live table:

- columns and normalized data types;
- nullability;
- default values;
- primary keys and generated-value sequences;
- foreign keys and `ON DELETE` behavior;
- unique constraints;
- check constraints and accepted value sets;
- indexes relevant to uniqueness or query behavior;
- numeric precision and scale;
- timestamp and time-zone semantics.

Resolve differences as follows:

- If HSQLDB is missing behavior that it supports, fix the corresponding test
  migration.
- If production is wrong, add `V38` or a later forward migration and its HSQLDB
  counterpart. Never repair production by editing an applied migration.
- If HSQLDB cannot represent PostgreSQL behavior exactly, document the limitation
  and cover the rule in the PostgreSQL migration test described below.

Known items that require explicit verification include negative price checks,
series occurrence uniqueness, nullable values in partial unique indexes, tournament
seed uniqueness, parent tournament-match uniqueness, enum-like value constraints,
and sequence names referenced by JPA entities.

## Phase 5: Add Regression Guards

### Migration Set Parity Test

Add a JUnit test in the persistence module that reads both migration directories and
fails when:

- a version is present in only one directory;
- the description for the same version differs;
- either directory contains duplicate versions;
- filenames do not follow Flyway's `V<version>__<description>.sql` convention.

The test should parse versions and descriptions rather than compare SQL contents.
PostgreSQL and HSQLDB SQL will legitimately differ.

The test must require the reserved `V16` in both directories. More generally, it
compares actual version sets and descriptions rather than assuming that all Flyway
histories must always use contiguous numbering.

### HSQLDB Constraint Tests

Add focused persistence tests for database behavior that Hibernate validation does
not prove. At minimum, verify rejection of:

- negative prices;
- invalid enum-like values;
- broken foreign-key references;
- duplicate values covered by business-critical unique constraints;
- incomplete match-series identity pairs;
- invalid coordinate pairs and ranges;
- invalid tournament registration and match schedule windows.

Each test should arrange its own data, execute one invalid operation, and assert the
database rejection. Do not test migration SQL by searching for text fragments.

### PostgreSQL Migration Smoke Test

HSQLDB cannot validate PostgreSQL syntax, enum conversion, partial indexes, or
sequence behavior. Add a persistence integration test that starts a clean PostgreSQL
database with Testcontainers and runs the real production migration directory.

The test must:

1. apply all production migrations to an empty PostgreSQL database;
2. call Flyway validation after migration;
3. start an EntityManagerFactory with `hbm2ddl.auto=validate` against that database;
4. exercise PostgreSQL-only constraints and partial unique indexes that HSQLDB
   cannot model exactly.

Declare Testcontainers versions in the parent POM and dependencies with test scope
in the persistence module. Implement this as a Failsafe integration test, such as a
`*IT` class, behind a clearly named Maven profile if Docker is not guaranteed for
every developer invocation. Use the existing PostgreSQL driver first; update it only
if the integration test proves that a driver change is required.

### Normalized Schema Comparison

In the PostgreSQL integration profile, migrate both a clean PostgreSQL database and
a clean HSQLDB database, then compare their normalized schemas with JDBC
`DatabaseMetaData`. JDBC is acceptable here because this is test infrastructure, not
production persistence code.

The comparison should cover:

- application table names;
- column names, normalized type families, precision, scale, and nullability;
- primary-key columns;
- foreign-key columns, referenced tables and columns, and delete rules;
- unique constraints that can be represented equivalently;
- expected sequence names used by JPA.

Maintain a small, explicit allowlist only for proven dialect differences, such as
PostgreSQL timestamp-with-time-zone metadata or partial-index representation. Every
allowlist entry must explain why behavioral equivalence is tested elsewhere. Do not
maintain a second hand-written schema manifest.

Check constraints, partial-index predicates, and database-specific defaults are not
reliably portable through `DatabaseMetaData`; verify those with targeted behavior
tests on the relevant database.

### Continuous Integration Gate

Add repository-host CI configuration appropriate to the project's hosting platform.
The required database job must have Docker support and run the PostgreSQL integration
profile on every pull request that changes:

- either migration directory;
- JPA entity mappings;
- persistence DAOs;
- the persistence or parent POM;
- migration verification tests.

It is acceptable to run the job on every pull request if path filtering is not
supported. A documented local command is useful but does not replace an automated
required check. Until this job is configured as a required merge check, the
regression-prevention part of this plan is incomplete.

## Phase 6: Verification Sequence

Run verification in this order so failures identify the responsible layer:

1. Run the migration-set parity test.
2. Migrate a fresh HSQLDB database and run persistence tests:
   `mvn -pl persistence -am test`.
3. Run the PostgreSQL migration integration profile against a fresh container:
   `mvn -Ppostgres-it -pl persistence -am verify`.
4. Run the complete project suite: `mvn test`.
5. Run the repository formatter check using the root Spotless configuration.
6. Inspect `git diff` and confirm that no production migration from the confirmed
   deployed history changed checksum.
7. Confirm that no generated database files, `target` directories, or container
   output are included in the commit.

The exact profile name may differ during implementation, but one stable command must
be documented in the project and enforced in CI.

## Delivery Order

Use small commits with reviewable responsibilities:

1. Add and deploy the reserved `V16` according to the dedicated backfill runbook.
2. Rebuild the HSQLDB migrations with matching versions and descriptions.
3. Add the migration-set parity test.
4. Add or update HSQLDB constraint tests.
5. Add the PostgreSQL Testcontainers migration test and Maven profile.
6. Add the normalized PostgreSQL/HSQLDB metadata comparison and dialect allowlist.
7. Add and require the repository-host CI database verification job.
8. Add any required forward-only production correction, together with its matching
   HSQLDB migration and tests.

Do not combine a production schema correction with historical production migration
rewrites. If a production correction is needed, its forward migration should remain
easy to identify and review.

## Rollback And Failure Handling

- The HSQLDB migration history is test-only and disposable. If rebuilding it fails,
  revert that test-only change and retry; no production database rollback is needed.
- Once a new production migration such as `V38` has been applied outside a local
  disposable database, do not delete or modify it. Correct it with another forward
  migration.
- A production checksum mismatch, a conflicting deployed `V16`, or a migration present
  in only some deployed environments blocks implementation until the ledger is
  reconciled.
- A PostgreSQL migration test failure blocks merge even if all HSQLDB tests pass.

## Definition Of Done

The correction is solved only when all of the following are true:

- Every persistent database has one successful `V16` history entry and out-of-order
  mode has been disabled after the controlled backfill.
- Production and test directories contain the same canonical version and description
  set.
- Every HSQLDB migration represents only its corresponding production change.
- No applied production migration was renamed, deleted, reordered, or changed.
- A fresh HSQLDB database migrates successfully and passes Hibernate validation.
- A fresh PostgreSQL database migrates using the real production scripts and passes
  Flyway and Hibernate validation.
- Normalized PostgreSQL and HSQLDB metadata matches except for documented,
  behavior-tested dialect differences.
- Important constraints are covered by behavior tests on HSQLDB and, where dialect
  behavior differs, on PostgreSQL.
- The automated migration-set parity test fails when a future migration is added to
  only one directory.
- The PostgreSQL migration profile is mandatory in CI for migration-related changes.
- The CI database job is configured as a required merge check, not merely documented.
- `mvn test`, the PostgreSQL migration verification command, and Spotless all pass.
