# Logging Guidelines (SLF4J + Logback)

This project uses **SLF4J API + Logback**.

## Runtime and test configuration

- Runtime config: `webapp/src/main/resources/logback.xml`
- Test config: `webapp/src/test/resources/logback-test.xml`
- WAR packaging explicitly excludes `WEB-INF/classes/logback-test.xml` (production always uses `logback.xml`)

Runtime uses `RollingFileAppender` with:

- `fileNamePattern`: `logs/paw-2026a-14.%d{yyyy-MM-dd}.log`
- retention (`maxHistory`): `5` days
- request correlation: `requestId` from MDC (header `X-Request-Id` or generated UUID)

Tests use `ConsoleAppender` to keep test output local and avoid creating files.

## Where logging belongs

Implement logging in modules with concrete behavior:

- `webapp` (controllers, security/auth entry points, global request outcomes)
- `services` (business decisions, rule outcomes, cross-DAO orchestration)
- `persistence` (unexpected DB failures and non-happy-path outcomes)

Do **not** add logging logic in:

- `models`, `service-contracts`, `persistence-contracts`
- DTO/form/view-model classes
- trivial getters/setters and pure mapping utilities

## Which classes should have a logger

Add a logger in:

- Spring entry points: controllers and security-related handlers/providers
- service implementations (`*ServiceImpl`)
- JDBC DAO implementations (`*JdbcDao`)
- components that call external systems (mail dispatch/rendering, integration adapters)

Avoid loggers in:

- interfaces
- value objects/entities
- classes with only deterministic pure transformations

## Standard level usage

- `TRACE`: high-frequency internals for deep debugging only (normally disabled)
- `DEBUG`: diagnostics useful during development/troubleshooting, no sensitive data
- `INFO`: meaningful business milestones (created, approved, cancelled, completed)
- `WARN`: recoverable anomalies or suspicious states (invalid transition, retry path)
- `ERROR`: operation failed and user flow/system integrity is affected

Rule of thumb: log **one layer per event**.  
If service logs the decision/result, controller should not repeat the same detail.

## Concrete examples

Use parameterized logging (`{}` placeholders), never string concatenation.

### Controller (`AuthController`)

```java
private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

LOGGER.info("Password reset requested for account id={} locale={}", accountId, locale);
LOGGER.warn("Registration rejected code={} emailHash={}", exception.getCode(), emailHash);
```

Notes:

- log stable identifiers (`accountId`, `matchId`) and outcome
- if email is needed for correlation, log masked/hashed version only

### Service (`MatchReservationServiceImpl`)

```java
private static final Logger LOGGER = LoggerFactory.getLogger(MatchReservationServiceImpl.class);

LOGGER.info("Reserve spot requested matchId={} userId={}", matchId, userId);
LOGGER.info("Reservation created matchId={} userId={}", matchId, userId);
LOGGER.warn("Reservation rejected code={} matchId={} userId={}", code, matchId, userId);
```

Notes:

- services are the main place to log business outcomes
- keep one success info log and one rejection/failure log per operation

### DAO (`MatchParticipantJdbcDao`)

```java
private static final Logger LOGGER = LoggerFactory.getLogger(MatchParticipantJdbcDao.class);

LOGGER.debug("Creating reservation row matchId={} userId={}", matchId, userId);
LOGGER.error("DB error while creating reservation matchId={} userId={}", matchId, userId, ex);
```

Notes:

- avoid logging raw SQL or large payloads
- include exception stack trace only on `ERROR`

## What must not be logged

Never log:

- passwords, password reset tokens, verification tokens
- authentication secrets, headers, session ids, remember-me cookies
- full email addresses, phone numbers, personal documents
- raw request bodies that may contain sensitive fields
- full HTML mail content or binary/image payloads

Prefer:

- ids and enum/status values
- masked/hashed identifiers when correlation is required

## Avoid overlogging and redundancy

- no duplicate logs across controller + service + DAO for same event
- no logs inside loops unless rate-limited and strictly necessary
- avoid `INFO` for every read/list operation
- use `DEBUG` for temporary diagnostics and remove or keep guarded by level
- do not use `System.out` / `System.err` for operational logs

## Testing and troubleshooting recommendations

- Keep `logback-test.xml` console-based and concise.
- Default tests to `WARN` root to reduce noise.
- Raise `ar.edu.itba.paw` to `DEBUG` only while debugging failing tests.
- For hard-to-reproduce issues:
  1. increase one package/class to `DEBUG`
  2. reproduce
  3. restore baseline levels
- Validate that logs contain enough correlation keys (`matchId`, `userId`, `requestId` when available) without exposing sensitive data.
