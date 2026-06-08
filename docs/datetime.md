# Date & Time Display

How this project stores, converts, and renders date/time values. The display rules
exist because the webapp runs on **JSTL 1.2 (`javax.servlet:jstl`)**, whose
`<fmt:formatDate>` only understands `java.util.Date`/`Calendar` and **cannot format
`java.time` types**. We therefore never use `<fmt:formatDate>`/`<fmt:parseDate>` on
domain timestamps; we use the custom EL functions described below.

## Storage and zone model

- Domain timestamps are stored as **`Instant`** (UTC) on the entities and as
  `TIMESTAMPTZ` in PostgreSQL.
- The single application zone is `PlatformTime.ZONE`
  (`America/Argentina/Buenos_Aires`), in `models/.../PlatformTime.java`.
- Conversions live only in `PlatformTime`:
  - `toInstant(LocalDate, LocalTime)` — wall-clock input → stored `Instant`.
  - `toOffsetDateTime(Instant)` — stored `Instant` → `OffsetDateTime` for views.

## Model getter contract

Every model with a timestamp exposes **two getters**:

```java
public Instant getStartsAt() { return startsAt; }                       // raw, for services/logic
public OffsetDateTime getStartsAtDateTime() {                           // for views/formatting
    return PlatformTime.toOffsetDateTime(startsAt);
}
```

- Views and the `tf:` functions consume the **`...DateTime`** getter (the
  `OffsetDateTime`), never the raw `Instant`.
- Controllers must **not** call `PlatformTime` to convert timestamps for views/forms;
  use the model's `...DateTime` getter instead.

## Displaying dates in JSP

Custom EL functions, declared in `webapp/src/main/webapp/WEB-INF/functions.tld`
(`uri="http://paw.itba.edu.ar/tags/time-functions"`), backed by static methods in
`webapp/.../utils/JspTimeFunctions.java`:

| Function       | Output                          | Backed by                          |
|----------------|---------------------------------|------------------------------------|
| `tf:date(t)`     | date only (`FormatStyle.MEDIUM`)  | `ViewFormatUtils.formatDate`         |
| `tf:dateTime(t)` | date + time (`MEDIUM` + `SHORT`)  | `ViewFormatUtils.formatDateTime`     |

Both accept any `java.time.temporal.TemporalAccessor` (so `OffsetDateTime`,
`LocalDate`, etc.), return `""` for `null`, and pick up the active locale from
`LocaleContextHolder` — no locale argument needed in the JSP.

Declare the taglib once per JSP and always wrap output in `<c:out>`:

```jsp
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
...
<c:out value="${tf:date(review.updatedAtDateTime)}" />
<c:out value="${tf:dateTime(match.scheduledStartsAtDateTime)}" />
```

This is the standard JSP tag-library facility — the same mechanism JSTL's own
`fn:` functions use — just declared as `<function>` rather than `<tag>`. It is the
sanctioned way to format `java.time` on this stack; it is not a workaround that
bends the rules.

Plain EL property access (`${dt.year}`, `${dt.dayOfMonth}`, `${dt.hour}`) works for
**conditionals/logic** but is **not** locale-aware — do not use it to build
user-facing display strings.

## Formatting helpers (`ViewFormatUtils`)

`webapp/.../utils/ViewFormatUtils.java` centralizes the formatters; all are
locale-aware and fall back to `Locale.ENGLISH` when no locale is available:

- `dateFormatter` — `ofLocalizedDate(MEDIUM)`
- `scheduleFormatter` — `ofLocalizedDateTime(MEDIUM, SHORT)`
- `timeFormatter` — `ofLocalizedTime(SHORT)`
- `formatInstant(Instant, Locale, ZoneId)` — for the rare case where an `Instant`
  must be formatted directly with an explicit zone.

Use localized `FormatStyle`s, never a hardcoded pattern, and never a fixed locale
such as `Locale.US`.

## Non-JSP surfaces (emails, flash messages, metadata, links)

JSP EL is unavailable, so resolve strings in Java using `ViewFormatUtils` with the
**recipient/active** locale (for async emails, the recipient's saved locale — not the
request-thread locale). Keep using `PlatformTime.ZONE` for zone-correct output.

## Input / binding (the round trip back in)

- Forms bind raw `LocalDate` / `LocalTime` (let Spring bind typed values; no manual
  trim/parse in controllers, and no `<fmt:parseDate>`).
- Controllers pass the bound `LocalDate`/`LocalTime` (or request-object fields)
  through **unchanged**; **services** own conversion to `Instant` via
  `PlatformTime.toInstant` and all time-related business rules.
- Bean Validation `ConstraintValidator`s may use `PlatformTime` for pre-submit
  "in the future" / ordering checks to give inline field errors; this is input
  validation, not the authoritative rule — the service re-validates.

## Adding a new timestamp field — checklist

1. Store as `Instant`; map the column as `TIMESTAMPTZ`.
2. Add both getters: `getFooAt()` (`Instant`) and `getFooAtDateTime()`
   (`OffsetDateTime` via `PlatformTime.toOffsetDateTime`).
3. In JSP, render with `${tf:date(model.fooAtDateTime)}` /
   `${tf:dateTime(model.fooAtDateTime)}` wrapped in `<c:out>`.
4. For emails/flash/metadata, format in Java via `ViewFormatUtils` with the
   correct locale.
5. Add/extend a test covering at least one non-default locale or zone-sensitive path.

## Do / Don't

- **Do** use `tf:date` / `tf:dateTime` on the `...DateTime` getters.
- **Do** keep all zone conversion inside `PlatformTime`.
- **Don't** use `<fmt:formatDate>` / `<fmt:parseDate>` on `java.time` values (fails
  on JSTL 1.2).
- **Don't** convert timestamps in controllers, or pass preformatted date strings from
  controllers to JSPs for ordinary view labels.
- **Don't** format with a fixed locale or a hardcoded pattern.
