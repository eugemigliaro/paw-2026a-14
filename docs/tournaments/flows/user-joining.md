# Flow — User Joining

The participant's complete journey from discovery to being locked into a team / pool, before the tournament begins. There are two parallel join paths, both available on the same tournament at the same time:

- **Solo** — player joins a pool; the host (via auto-bundling) groups solos into teams when registration closes.
- **Team draft** — a captain invites N–1 friends; the team locks once 5/5 accept.

> Wireframe references in this doc: §2 (discover), §3 (solo), §4 (team draft).

---

## 1. Discovering a tournament

### Entry points

- The main event feed. Tournaments appear inline with regular events, differentiated only by a small `🏆 TOURNAMENT` badge on the card (see [ui-patterns.md → feed card](../ui-patterns.md#tournament-card-in-feed--same-as-event-card)).
- The `🏆 Tournaments` filter chip on the feed (filters to type=tournament only).
- Direct URL share.
- Search (when a user searches for a sport or location that matches a published tournament).

### The detail page

The tournament detail page is structured top-to-bottom:

1. **Hero banner** + title + sport + skill level
2. **Status eyebrow** — "🏆 TOURNAMENT · REGISTRATION OPEN" (or LIVE / COMPLETED, etc.)
3. **Quick facts panel** — closes-at countdown, play-dates range, venue, single-elim format, team-size
4. **Two parallel CTAs**:
   - Primary: "Join solo"
   - Secondary: "Start a team draft"
5. **Teams forming list** — read-only list of teams already locked in, plus drafts in progress (with `n/5` counts), plus the solo pool count

If the tournament is full (8 locked teams) the CTAs disappear and a "Registration closed" message replaces them.

If the user is already participating, the CTAs are replaced with "You're in!" status + a "Manage your participation" link (which deep-links to either their team draft, their solo entry, or their locked team — whichever applies).

### Acceptance criteria

- The detail page is publicly viewable (auth not required for read).
- Joining requires auth — clicking either CTA while logged out redirects to login and back.
- The "Teams forming" list updates without full page reload as drafts lock in (poll or Turbo-stream — implementer's choice).

---

## 2. Joining solo

### Steps

**Step A — Confirm**

Tapping "Join solo" opens a confirmation modal:

- Title: "Join Spring Cup as a solo player?"
- Body: "You'll go into the solo pool. The host will place you on a team that still has open spots before the bracket draws."
- Position context: "Position right now: 5th solo · pool has 1 open team slot"
- Buttons: Cancel / "Join the pool" (primary)

There is **no email confirmation step**. The platform has proper auth; the join is instant once the user clicks "Join the pool".

**Step B — Joined**

Page reloads (or modal closes + state refreshes) showing:

- Success banner: "✓ You're in." + "Spring 5-a-side Cup · solo pool. We'll notify you when you're placed on a team."
- A small subtitle: "✉ Confirmation sent to {email} · 🔔 also in your inbox"
- "What's next" panel with bracket-draw date and what will happen
- Two buttons: "← View tournament" (back to detail) and "Leave the solo pool" (destructive, secondary)

### Acceptance criteria

- The "Join the pool" action is idempotent — a user already in the pool sees the success state immediately without an error.
- "Leave the solo pool" works at any point during the REGISTRATION phase. After phase changes to LOCKED_BRACKET, the button is hidden.
- The dual-channel notification fires immediately: in-app record + email.

### Edge cases

- A user who is in a team draft and tries to join solo: block with "You're already on a team in this tournament" — see [open-questions.md → user in a draft + accepts another invite](../open-questions.md#a-user-is-in-a-draft--accepts-another-drafts-invite-blocking).
- A user who joins late (within 24h of registration close) when the pool already exceeds capacity for the synthesized teams: still allow joining; show "You may not be placed on a team if the pool overflows" caveat in the modal.

---

## 3. Starting a team draft

### Captain side

**Step A — Start the draft**

Tapping "Start a team draft" opens a screen (full page, not modal):

- Title: "New team draft" + subtitle "{TOURNAMENT NAME} · needs 5 players"
- Team name field (required)
- Invite-teammates section: list of `@username` entries + an add-row input that accepts `@username` or an invite link
- Counter strip: "You + 4 invites = 5/5"
- Helper: "Team locks in when everyone accepts. If anyone declines, you can swap them out."
- Primary CTA: "Send invites & start draft"
- Secondary: "Cancel"

The captain can save the draft with fewer than `team_size - 1` invites and add more later, OR submit with the full roster all at once. The form supports both.

On submit:

1. A `TournamentTeamDraft` row is created with status OPEN.
2. A `TournamentDraftInvite` row is created per invitee with status PENDING.
3. Each invitee receives a dual-channel notification (in-app + email).

**Step B — Monitor the draft**

After starting the draft, the captain lands on a "Draft status" screen:

- Title: "{Team name} · draft" + "{N}/5 confirmed — need {5-N} more to lock the slot"
- Player roster list showing each invitee's status:
  - Captain row: "You (Diego) · ★ Captain"
  - Accepted: "@mariana_p · ✓ Accepted"
  - Pending: "@elcapo · ⏳ Pending"
  - Declined: "@andres · ✕ Declined · swap?"
- "+ Invite another player" CTA (always available as long as the slot is open)
- Tinted info: "Team locks in when 5/5 accept. Then you get an official slot — visible to everyone on the tournament page."
- "Disband draft" (destructive)

The captain can:

- Click "swap?" on a declined invitee → opens an invite-modal to replace them.
- Send additional invites if the team isn't full yet.
- Disband the draft at any time (releases all accepted players, fires notification).

**Step C — Team locks in**

When the Nth (last) invitee accepts, automatically:

1. The draft status flips to LOCKED.
2. A new `TournamentTeam` row is created with the draft's roster and name.
3. The tournament's open-slot count decrements by 1.
4. Every team member (captain included) receives a "Team locked in" notification.
5. The captain (and everyone else) sees a celebratory state on next page load:
   - Banner: "🔒 LOCKED IN · {Team name} is in the bracket."
   - Subtitle: "Slot N/8 · {Tournament name}"
   - Squad list
   - "What's next" panel with bracket-draw date

### Invitee side

The invitee's path is symmetric with how the existing match invitations work — see `MatchParticipationServiceImpl.dispatchMatchInvitation` for the dual-channel pattern.

**Step 1 — Notification arrives** (in-app + email simultaneously)

The in-app notification appears in the bell dropdown with a `1` badge. The email arrives in parallel with the same content and a CTA that opens the in-app view.

**Step 2 — Invite review screen**

Tapping the notification opens an invite-review screen:

- Eyebrow: "TEAM INVITE"
- Title: "Diego invited you to Los Galácticos"
- Subtitle: "For {Tournament name}. Plays {date range} at {venue}."
- Two buttons: Accept (primary) / Decline (secondary)
- Below: "Team status: 3/5 confirmed · 1 pending · 1 declined" (so they know what they're walking into)

Accept and Decline are immediate. Either action fires a notification back to the captain (dual-channel).

### Acceptance criteria

- A user can be in at most one team or in the solo pool per tournament. The service layer enforces this on every join/accept action.
- The draft "Lock in" check runs every time an invitee accepts — race conditions where two invitees accept simultaneously should still result in a single LOCKED transition.
- An invitee can only accept once. After their first response, the invite is read-only with their decision shown.
- A captain can invite the same user back after they decline (creates a new invite row, original stays declined for audit).

### Edge cases

- Captain invites a user who isn't on the platform: out of scope for v1 (no email-only invites). The invite input only accepts existing `@username`s.
- Captain invites themselves: block with "You're already on this team".
- A team draft hasn't reached `team_size` accepted invites by the time registration closes: the draft is disbanded automatically, all accepted players are notified, and the slots become available for solo-pool synthesis. See [open-questions.md → under-capacity registration close](../open-questions.md#under-capacity-registration-close-blocking).
- Captain abandons their own draft (clicks "Disband draft"): see [open-questions.md → captain leaves their own team](../open-questions.md#captain-leaves-their-own-team-mid-registration-blocking).

---

## Combined "joined" state — what each user sees on the detail page

After joining (either path):

| Path | Detail-page CTA replaces with | Side panel shows |
|---|---|---|
| Solo pool | "You're in the solo pool" | "Manage your participation" |
| Captain of draft (open) | "Your draft: 3/5 confirmed" | "View draft" link |
| Captain of draft (locked) / team member of locked team | "Your team: Los Galácticos · slot 2/8" | "View team" link |
| Invitee with pending invite | "You have a pending invite" | "Respond" link |

The list of "Teams forming" continues to show others' progress so the user has context for the overall tournament.
