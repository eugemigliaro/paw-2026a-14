# Tournament Teams — Join via Self-Organized Teams

**Date:** 2026-06-09
**Branch:** `feat/join-tournament-with-team`
**Status:** Design — approved for spec review

## 1. Summary

Let players who want to join a tournament **create a team and have others join it**, in
addition to the existing solo queue. Teams have **no owner**: anyone may join or leave at
any time while registration is open, and a team that loses its last member is deleted.

This is an **additive** change. The existing solo-queue path (`joinSolo` / `leaveSolo` and
`TournamentSoloEntry`) stays intact; the two paths coexist and share one capacity budget.

## 2. Decisions (locked in)

1. **Additive, not a replacement.** Solo queue and self-organized teams both exist, gated by
   the existing `Tournament.allowSoloSignup` / `Tournament.allowTeamDraft` flags respectively.
2. **No captain.** User-created teams use the existing `TournamentTeam` with
   `origin = TEAM_DRAFT`, and every `TournamentTeamMember` is created with `captain = false`.
3. **Team name required.** The creator must supply a non-blank name. Per-tournament name
   uniqueness is **not** enforced at this stage (the column is already nullable as of V37; no
   migration needed). Optional `UNIQUE(tournament_id, name)` is noted as possible future work.
4. **Capacity is a per-person budget, shared across both paths.** Total capacity =
   `bracketSize × teamSize` **people**, counting every active solo-queue entry plus every team
   member (whether their team is full or not). A *team* reserves nothing; a *person* consumes
   one unit the moment they register anywhere. Creating or joining a team is allowed only while
   registered people `< capacity`. This replaces the earlier "cap at bracketSize teams" idea,
   which allowed 1-person teams to squat bracket slots.
   - Consequence: at most `floor(capacity / teamSize) = bracketSize` full teams can ever form,
     so the bracket always holds every full team assembled at close. No "excess complete teams"
     case is possible.
   - First-come semantics: when the tournament is full, further registration is rejected
     regardless of how existing registrants are grouped (40 one-person teams = a full 40-person
     tournament, identical to 40 solo-queue entries).
5. **At close, incomplete teams are salvaged, not blindly dropped** (see §5).
6. **Unplaced (`UNASSIGNED`) players are emailed** (Option A, see §6). Today they receive no
   notification at all — not even the bracket-published email, because email recipients are
   drawn only from `TournamentTeamMember`s.

## 3. Scope

### In scope
- New service operations: `createTeam`, `joinTeam`, `leaveTeam`, `listJoinableTeams`.
- New DAO operations: `removeMember`, `delete`, `countMembers`, `countMembersByTournament`,
  `findJoinableByTournament`.
- Unified capacity check shared by `joinSolo`, `createTeam`, `joinTeam`.
- `closeRegistration` change: salvage incomplete teams via top-up + repack (§5).
- "Not placed" email + i18n templates (§6).
- Web layer: create/join/leave endpoints, a joinable-teams list on the tournament page, and
  new flags on the registration state (§7).
- Tests for all of the above.

### Out of scope
- No new `Team` entity, no standalone teams outside a tournament.
- No captain powers / team ownership / rename / kick.
- No waitlist or priority for pre-formed groups over singletons (first-come wins).
- No per-tournament team-name uniqueness constraint.
- No change to bracket generation, match play, or seeding beyond what close already does.

## 4. Architecture — new vs. existing

**No new service, DAO, or entity type is introduced.** Everything extends existing components.

### 4.1 Models (`models`) — reuse
- `TournamentTeam` with `origin = TEAM_DRAFT`.
- `TournamentTeamMember` with `captain = false`.
- New exceptions under `models/.../exceptions/tournamentRegistration/`:
  - `TournamentRegistrationTeamDraftDisabledException` (mirrors the solo-signup-disabled one)
  - `TournamentRegistrationTournamentFullException` (shared capacity reached)
  - `TournamentRegistrationTeamNotFoundException`
  - `TournamentRegistrationTeamFullException`
  - `TournamentRegistrationNotOnTeamException`
  - `TournamentRegistrationAlreadyInSoloPoolException` (mutual-exclusion on join/create)
  - Reuse existing `TournamentRegistrationAlreadyOnTeamException`,
    `TournamentRegistrationNotOpenException`, `TournamentRegistrationUnderCapacityException`.

### 4.2 Services (`service-contracts` + `services`) — extend `TournamentRegistrationService`
This service already owns `joinSolo`/`leaveSolo`, already injects
`TournamentTeamDataService`, and already enforces registration-open / capacity rules. Per
AGENTS.md ("a service uses its own aggregate DAO and calls other services for other
aggregates"), tournament registration is the correct aggregate boundary. A standalone
`TeamService` would re-inject the same dependencies and duplicate the same checks — rejected.

New interface methods:
```
TournamentTeam       createTeam(long tournamentId, User user, String name)
TournamentTeamMember joinTeam(long tournamentId, long teamId, User user)
void                 leaveTeam(long tournamentId, User user)
List<TournamentTeam> listJoinableTeams(long tournamentId)
```

Validation (reusing existing `validateUser`, `requireRegistrationOpen`, and the
findUserTeam/findSoloEntry helpers):
- **All three writes:** user non-null; registration open; `tournament.isAllowTeamDraft()`.
- **Mutual exclusion:** the user must not already be on a team
  (`AlreadyOnTeamException`) and must not be `IN_POOL` in the solo queue
  (`AlreadyInSoloPoolException`) — symmetric with how `joinSolo` already blocks team members.
- **createTeam:** non-blank name; tournament not at capacity; create `TEAM_DRAFT` team, then
  immediately `addMember(team, user, false)` so the creator is a member.
- **joinTeam:** team exists and belongs to this tournament; team not full
  (`countMembers(teamId) < teamSize`); tournament not at capacity.
- **leaveTeam:** user is on a team in this tournament; `removeMember`; if
  `countMembers(teamId) == 0` afterward, `delete` the team.

Capacity is a single private helper, e.g. `isAtCapacity(tournament)` =
`activeSoloEntries(tournamentId) + teamMembers(tournamentId) >= bracketSize × teamSize`,
used by `joinSolo`, `createTeam`, and `joinTeam`. The existing solo-only `isSoloPoolFull`
check is generalized to this shared occupancy check (its public method keeps working but now
reflects total occupancy).

`getRegistrationState` gains `canCreateTeam` / `canJoinTeam` / `canLeaveTeam` flags, computed
the same way the existing `canJoinSolo` / `canLeaveSolo` flags are.

### 4.3 Persistence (`persistence-contracts` + `persistence`) — extend the team DAO
`TournamentTeamDao` (and its mirror internal `TournamentTeamDataService`) already expose
`create`, `addMember`, `findById`, `findUserTeam`, `findByTournament`, `countByTournament`.
Add (to both the contract and the internal data-service interface, kept in sync):
```
void removeMember(TournamentTeam team, User user)
void delete(TournamentTeam team)
long countMembers(long teamId)
long countMembersByTournament(long tournamentId)         // for shared capacity
List<TournamentTeam> findJoinableByTournament(long tournamentId, int teamSize)  // member count < teamSize
```
`findJoinableByTournament` and any member-count display must come from **DAO queries**, not
in-memory filtering/counting (AGENTS.md). The joinable-teams list needs member counts per team
for the UI ("3/5"); resolve this with a single grouped count query (e.g. a small projection of
`teamId → count`) — no N+1 `countMembers` loop.

### 4.4 Migrations
None required for the data model — `tournament_teams` / `tournament_team_members` already exist
and the name column is nullable. (Optional future `UNIQUE(tournament_id, name)` = V38.)

## 5. `closeRegistration` change — salvage incomplete teams

Today `closeRegistration` (`TournamentRegistrationServiceImpl:217`) counts existing teams toward
the bracket but never checks whether they are full, and packs only the solo pool. New algorithm:

1. **Keep complete teams** (`countMembers == teamSize`) as-is.
2. **Top up incomplete teams from the solo queue**, closest-to-full first, adding `IN_POOL`
   players until each reaches `teamSize` or the solo queue is exhausted. Topped-up players move
   to `ASSIGNED` with their `assignedTeam` set, exactly as the current solo-assign code does.
3. **Dissolve still-incomplete teams**: any team below `teamSize` after top-up is `delete`d and
   its members returned to the pool.
4. **Pack the pool** (leftover `IN_POOL` solo players + members of dissolved teams) into fresh
   full teams of `teamSize`, reusing the existing packing loop.
5. **Leftovers** → `UNASSIGNED`, exactly as today.
6. **Under-capacity guard** unchanged: if fewer than 2 full teams result, throw
   `TournamentRegistrationUnderCapacityException`.

The capacity cap (§2.4) guarantees the number of full teams never exceeds `bracketSize`, so no
slot accounting / team dropping for lack of space is needed.

Worked example — `teamSize = 5`, one team "Bravo" at 3 members, solo queue = 4:
Bravo gets +2 (→ 5, kept); remaining 2 solo players can't form a team of 5 → `UNASSIGNED`.

## 6. "Not placed" email (Option A)

Add to `TournamentMailService` (+ impl, + the underlying `MailDispatchService` send method):
```
void sendNotPlacedEmail(Tournament tournament, User user)
```
Called from the close flow once per `UNASSIGNED` user, after team assembly. Requirements
(AGENTS.md):
- **Recipient locale**, not the request-thread locale — render each email in the recipient's
  `preferredLanguage`.
- New i18n keys in **both** `messages.properties` and `messages_es.properties`.
- Friendly framing ("Registration closed and you weren't placed in a team this time"), with a
  CTA back into the app (e.g. browse other open tournaments). No "didn't make the cut" wording.
- Mail orchestration lives in the service layer (reuse `TournamentMailService`), never in a
  controller or DAO.

Note: this is a new outbound email; placed players continue to receive the existing
bracket-published email through the unchanged `recipients()` path.

## 7. Web layer (brief)

- Controller endpoints for create / join / leave team (POST, redirect back to the tournament
  page), binding a small form object for the team name with Bean Validation (`@NotBlank`).
- Tournament page renders `listJoinableTeams` (name + "members/teamSize") and the user's current
  team, driven by the new registration-state flags. Controllers stay thin: one service call,
  choose view/redirect, map binding errors — no ownership/capacity logic in the controller.
- All new copy in both message bundles; URLs via `<c:url>`/`<c:param>`.

## 8. Testing

- **Service (JUnit 5 + Mockito, no `verify`, one scenario per test, Arrange/Exercise/Assert):**
  createTeam happy path + name-blank + team-draft-disabled + at-capacity + already-on-team +
  already-in-solo; joinTeam happy + team-full + team-not-found + at-capacity; leaveTeam happy +
  last-member-deletes-team + not-on-team.
- **`closeRegistration`:** top-up completes a near-full team; still-incomplete team dissolved and
  repacked; leftovers `UNASSIGNED`; under-capacity throws.
- **Persistence (Spring Test + HSQLDB):** `removeMember`, `delete`, `countMembers`,
  `countMembersByTournament`, `findJoinableByTournament` (excludes full teams), set up and assert
  state independently of the method under test.
- **Mail/locale:** the not-placed email renders in a non-default recipient locale.
- **Controller/route:** create/join/leave require auth and registration open; denial paths.

## 9. Open questions

- None blocking. Optional future items: per-tournament unique team names; allowing an
  `UNASSIGNED` player to drop back into the solo queue for a *future* tournament; priority for
  pre-formed groups over singletons when near capacity.
