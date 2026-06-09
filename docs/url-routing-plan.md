# Canonical Match And Tournament URLs

## Summary

- Replace the single authenticated "My events" area with two nav destinations:
  - `/matches` labeled "My matches"
  - `/tournaments` labeled "My tournaments"
- Redirect legacy list URLs:
  - `/events` and `/events?type=match` to `/matches`
  - `/events?type=tournament` to `/tournaments`
- Replace create-page URLs:
  - `/host/matches/new` redirects to `/matches/new`
  - `/host/tournaments/new` redirects to `/tournaments/new`
- Do not add explicit back/cancel links; browser history remains the back behavior.

## Key Changes

- Update `MatchDashboardController` with separate `GET /matches` and `GET /tournaments` list handlers, forcing the event type from the path instead of trusting `type`.
- Update list URL generation so search, filters, sort, pagination, and the match/tournament toggle use `/matches` or `/tournaments` without `type=...`.
- Update the authenticated header nav:
  - Replace `nav.player.events` with two links, `nav.player.matches` and `nav.player.tournaments`.
  - Make `/matches` and `/tournaments` active independently.
  - Keep host CTAs separate, pointing to `/matches/new` and `/tournaments/new`.
- Add English and Spanish i18n keys:
  - `nav.player.matches=My matches`
  - `nav.player.tournaments=My tournaments`
  - `nav.player.matches=Mis partidos`
  - `nav.player.tournaments=Mis torneos`
  - Add matching list/page title keys if the `/matches` and `/tournaments` pages should no longer show `events.title=My events`.
- Move canonical create submissions:
  - Match create form posts to `/matches/new`.
  - Tournament create form posts to `/tournaments`.
- Keep existing host management routes unchanged for edit, cancel, bracket setup, requests, invites, and winner updates.

## Security

- Update `SecurityConfig` before the public detail-route wildcards:
  - Require `USER` or `ADMIN_MOD` for `GET /matches`, `GET /tournaments`, `GET /matches/new`, `POST /matches/new`, `GET /tournaments/new`, and `POST /tournaments`.
  - Keep public detail pages like `GET /matches/{id}` and `GET /tournaments/{id}` public.
- Do not add `@PreAuthorize`; route protection stays centralized in `SecurityConfig`.

## Tests

- Update controller tests for `/matches`, `/tournaments`, legacy redirects, query preservation, invalid binding, and forced event type by route.
- Update host create tests for new GET/POST URLs and form actions.
- Update `SecurityConfigTest` for anonymous and authenticated access to the new list and create routes.
- Update view/template tests to assert the two nav labels and remove expectations for `nav.player.events` and `type=tournament` canonical links.
- Run `mvn test`.

## Assumptions

- Legacy GET URLs redirect to canonical URLs.
- No generated UI should point to old `/host/.../new` create routes.
- Browser back behavior is enough; no explicit back/cancel control will be added.
