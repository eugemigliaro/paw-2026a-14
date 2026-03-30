# DESIGN.md

## 1. Overview

This design system defines the visual and interaction language for the sports events platform shown in the current mocks. The product should feel **premium, social, clean, and active**. It is not meant to look like a loud fitness app or a generic marketplace. It should feel closer to a modern sports club: calm, trustworthy, organized, and slightly aspirational.

The UI should communicate three things at all times:

1. **Discoverability** — users should quickly understand what events exist, when they happen, and whether they can join.
2. **Confidence** — the product should feel reliable enough for payments, reservations, host management, and event logistics.
3. **Community** — the interface should quietly reinforce that these are real people, real venues, and recurring activities.

---

## 2. Creative North Star

### The Premium Sports Clubhouse

The overall aesthetic should feel like a digital version of a premium local club or curated community platform.

Key characteristics:

- Spacious, editorial composition
- Refined emerald-driven palette
- Soft neutral backgrounds
- Clear card-based information architecture
- Strong hero imagery for events and venues
- Minimal visual noise
- Friendly but not playful typography
- Light social proof details that never overpower primary actions

The interface should feel polished and premium, but still approachable for everyday use.

---

## 3. Relationship to the Previous Reference

This file is based on the current sports mocks and also adapts the strongest structural ideas from the previous reference design document:

- keep the emphasis on **whitespace, tonal layering, and premium composition**
- keep the preference for **clean surfaces instead of cluttered dividers**
- keep the idea that **imagery is the emotional anchor and the UI is the frame**

However, unlike the older reference, this version should be more functional and product-driven:

- sports discovery and booking require **clearer controls**
- restrained 1px outlines are acceptable for **inputs, outlined buttons, and utility controls**
- layout should feel more like a **premium marketplace / club platform** than a luxury editorial magazine

---

## 4. Brand Attributes

Use these words as a filter when making design decisions:

- Premium
- Active
- Social
- Calm
- Trustworthy
- Modern
- Structured
- Airy

Avoid directions that feel:

- Neon
- Aggressive
- Overly sporty
- Game-like
- Noisy
- Corporate and cold
- Over-decorated

---

## 5. Color System

The visual system is anchored around an emerald palette with soft neutrals.

### Core palette

- **Primary:** `#00645A`
- **Secondary / strong dark neutral:** `#333333`
- **Tertiary / mint accent:** `#CCFFD0`
- **Neutral base:** `#F2F2F0`

### Semantic usage

#### Primary
Use for:
- main CTA buttons
- selected chips
- active nav underline or active state
- key icons
- spot-availability badges
- important emphasis inside booking flows

#### Secondary
Use for:
- primary text on light surfaces when extra weight is needed
- inverted button backgrounds
- icon/text contrast on light neutral areas

#### Tertiary
Use for:
- supportive accents
- positive confirmation surfaces
- soft badges like “bookings”, “best value”, or success confirmation areas
- small highlight backgrounds in feature callouts

#### Neutral
Use for:
- app background
- section backgrounds
- large canvas areas
- forms and layout scaffolding

### Surface hierarchy

Use surfaces to create depth before relying on borders.

- **App background:** off-white / soft neutral
- **Primary cards:** white or near-white
- **Inset sections:** slightly darker neutral than cards
- **Utility panels:** white with subtle separation
- **Positive / supportive panels:** tinted mint backgrounds

### Rules

- Emerald should be the **main accent**, not the entire page.
- Large content areas should remain **light and breathable**.
- Use mint sparingly, mostly for positive states or supportive highlights.
- Avoid introducing new saturated colors unless needed for status semantics.
- Red may be used only for destructive actions such as cancel/delete.
- Do not use pure black for body text; prefer a softened dark neutral.

---

## 6. Typography

### Primary typeface

Use **Plus Jakarta Sans** as the core typeface across the entire product.

It fits the mocks because it feels:
- modern
- geometric but not cold
- premium without being luxury-fashion
- readable at both display and UI sizes

### Typographic tone

Typography should balance:
- strong, confident headlines
- soft readable body copy
- compact metadata labels

### Recommended scale

#### Display / hero
- **Display XL:** 64 / 72, weight 800
- **Display L:** 56 / 64, weight 800
- **Hero title:** 48 / 56, weight 800

#### Headings
- **H1:** 40 / 48, weight 800
- **H2:** 32 / 40, weight 700
- **H3:** 24 / 32, weight 700
- **H4:** 20 / 28, weight 700

#### Titles / card headers
- **Title L:** 22 / 30, weight 700
- **Title M:** 18 / 26, weight 600
- **Title S:** 16 / 24, weight 600

#### Body
- **Body L:** 18 / 30, weight 400
- **Body M:** 16 / 26, weight 400
- **Body S:** 14 / 22, weight 400

#### Labels / metadata
- **Label L:** 14 / 20, weight 600
- **Label M:** 12 / 16, weight 600
- **Label S:** 11 / 14, weight 600

### Typography rules

- Headlines should feel bold and decisive.
- Body text should remain soft and highly readable.
- Metadata labels may be uppercase when they represent categories, statuses, or event tags.
- Small labels can use slight positive letter spacing.
- Avoid overly tight text blocks. Let the layout breathe.

---

## 7. Layout Principles

### 7.1 Spaciousness over density

The interface should use generous spacing to create a premium feel. Screens should never feel cramped.

### 7.2 Strong content hierarchy

Each screen should have:
- one clear primary action
- one obvious reading path
- one dominant visual anchor

### 7.3 Mixed marketplace + editorial composition

The product is functional, but the layout should still feel composed. Use:
- wide hero areas
- large headlines
- centered search on discovery
- asymmetric two-column layouts for detail and create flows
- clean card grids for browsing

### 7.4 Visual rhythm

Alternate between:
- large open areas
- structured cards
- short metadata clusters
- prominent CTAs

This rhythm prevents the UI from feeling like a spreadsheet.

---

## 8. Grid, Spacing, and Radius

### Grid

#### Desktop
- 12-column layout
- generous page gutters
- left filter rail allowed on exploration pages
- 2-column content + side panel layout for detail and host flows

#### Tablet
- collapse sidebars below main content
- reduce card columns
- preserve large headings and imagery

#### Mobile
- 4-column rhythm
- stack detail layouts vertically
- turn side panels into bottom sections or sticky bottom bars
- chips should scroll horizontally when needed

### Spacing scale

Use an 8px-based system:

- 4
- 8
- 12
- 16
- 24
- 32
- 40
- 48
- 64
- 80

Preferred usage:
- tight UI spacing: 8–16
- standard component spacing: 16–24
- section spacing: 32–64
- hero separation: 64–80

### Radius

The UI should feel soft and premium, not sharp.

- **Small controls:** 10–12px
- **Buttons / inputs:** 12–16px
- **Cards:** 18–24px
- **Pills / chips:** full rounded
- **Large image cards / panels:** 24px

---

## 9. Elevation, Borders, and Separation

### General rule

Prefer separation through:
- background contrast
- spacing
- card containment
- soft shadow
- tonal shifts

Do not rely heavily on divider lines.

### Borders

Allowed uses:
- inputs
- outlined buttons
- low-emphasis utility controls
- upload dropzones
- subtle panel separation where needed

Border style:
- 1px
- low-contrast neutral gray
- never harsh black

### Shadows

Use shadows sparingly.

Preferred shadow style:
- soft
- diffused
- low opacity
- optionally slightly tinted toward the green family for premium warmth

Cards should often feel elevated because they sit on a softer neutral field, not because they cast dramatic shadows.

---

## 10. Iconography

Use a clean outline icon set with a modern technical feel.

The icon style should be:
- thin-to-medium stroke
- rounded terminals
- geometrically clean
- readable at small sizes
- consistent across navigation, metadata, filters, and status indicators

Good fit:
- outline icons similar to Lucide

### Icon usage rules

- default icon size: 18–22px in dense UI, 24px in standard UI
- use primary emerald for important active icons
- use dark neutral for passive icons
- use red only for destructive actions
- avoid mixing filled and outline systems arbitrarily

---

## 11. Imagery

Imagery is a core part of the product.

### Use photography for:
- event hero banners
- event cards
- venue previews
- reservations
- recommendation cards

### Preferred image style

- bright natural lighting
- realistic venues
- premium but believable sports environments
- clean compositions
- high clarity
- minimal clutter
- a mix of people, courts, equipment, and environments

### Visual direction by surface

#### Explore cards
Use energetic but clean visuals that quickly signal the sport.

#### Event detail hero
Use immersive, cinematic venue or match imagery.

#### My Events / reservations
Use more direct, legible imagery with strong relevance to the event.

#### Empty / placeholder states
Use muted sports illustrations or simplified venue graphics.

### Rules

- Photography should support trust and aspiration.
- Avoid stock that feels fake, oversaturated, or too commercial.
- Prefer consistency in tone, cropping, and image quality.

---

## 12. Core Interaction Principles

### Clarity first
Every primary action should be immediately identifiable.

### Gentle emphasis
Use contrast, size, and placement before animation.

### Low-friction exploration
Search, chips, filters, and cards should feel effortless to scan.

### Confidence in transactions
Booking panels, prices, availability, and event details should be structured with maximum clarity.

---

## 13. Component Guidelines

## 13.1 Top Navigation

The top navigation should be minimal, airy, and premium.

### Structure
- logo on the left
- primary navigation links near the left/center
- account actions and key CTA on the right

### Behavior
- active section indicated with underline or strong color emphasis
- host CTA should remain obvious
- utility icons should be visually quieter than the main CTA

---

## 13.2 Primary Buttons

### Style
- emerald background
- white text
- medium-to-large horizontal padding
- rounded corners
- minimal or no shadow by default

### Usage
Use for:
- Find Matches
- Host Event / Create Event
- Reserve a spot
- View Ticket
- Publish Event

### States
- hover: slightly darker emerald or slightly stronger elevation
- active: deeper emerald
- disabled: lower contrast and reduced emphasis

---

## 13.3 Secondary Buttons

Use for less important actions such as:
- outlined support actions
- rebook
- utility actions
- alternate flow steps

Style:
- white or neutral surface
- subtle border
- dark text
- same radius family as primary

---

## 13.4 Destructive Buttons

Use only for:
- cancel reservation
- delete event
- dangerous confirmation actions

Style:
- white or neutral surface
- red text
- subtle border or quiet destructive emphasis
- never make destructive actions more visually dominant than the primary CTA

---

## 13.5 Search Bar

The search bar is a major hero element on the Explore screen.

### Characteristics
- wide and centered
- large enough to feel like the entry point to discovery
- rounded rectangle
- subtle border or surface separation
- search icon on the left
- integrated CTA button on the right for desktop

### Tone
It should feel calm, inviting, and high-confidence.

---

## 13.6 Filter Chips

Chips are essential for category and browse refinement.

### Selected state
- emerald background
- white text

### Unselected state
- light neutral fill
- dark neutral text

### Rules
- keep them pill-shaped
- allow horizontal wrap or scroll depending on screen size
- selected chips should stand out clearly without feeling loud

---

## 13.7 Sidebar Filters

The left rail on Explore should feel simple and structured.

### Sections
- categories
- timeframe
- price
- skill level

### Rules
- headings should be small and uppercase or label-style
- items should use icon + text when useful
- spacing should do most of the grouping work
- selected row can use filled emerald background
- other rows should remain quiet and easy to scan

---

## 13.8 Event Cards

Event cards are one of the core product units.

### Anatomy
- image
- status badge (spots left / full)
- category label
- price
- title
- date/time
- location
- subtle participant/social proof
- CTA

### Visual direction
- white card on neutral background
- rounded corners
- clean vertical rhythm
- strong image at top
- content area should feel structured but not boxed-in

### Rules
- titles should wrap gracefully
- metadata icons should stay subtle
- CTA should not overwhelm the card
- badges should sit cleanly on imagery, usually top-right
- avoid visual clutter in the bottom row

---

## 13.9 Reservation Cards

Used on the My Events screen for upcoming reservations.

### Structure
- wide horizontal card
- image on left
- title + location in center
- date block or event timing on the right side of content
- primary and destructive/support actions aligned clearly

### Tone
These cards should feel important, reliable, and calm.

---

## 13.10 Activity Rows

Used for past activities.

### Style
- low-emphasis white row
- small thumbnail
- compact metadata
- right-aligned utility actions

### Goal
Users should be able to scan history quickly without the screen feeling heavy.

---

## 13.11 Form Sections

Create Event uses large contained form sections.

### Pattern
Each section should feel like a modular step:
- numbered badge
- section title
- grouped fields
- consistent internal spacing

### Section behavior
- section cards sit on the main neutral background
- fields inside should be highly legible
- avoid crowded multi-column forms unless the fields are closely related

---

## 13.12 Summary Rail / Side Panel

The right-side event summary panel on Create Event and the booking card on Event Detail are high-priority conversion components.

### Characteristics
- visually distinct from main content
- strong surface contrast
- clear CTA
- concise supporting text
- confidence-building microcopy

### Rules
- keep this panel sticky on desktop when helpful
- do not overload it with secondary actions
- use mint for reassurance and supportive callouts

---

## 13.13 Status Pills and Badges

Used for:
- spots left
- best value
- bookings count
- sport labels
- positive confirmations

### Style
- rounded pill
- compact label typography
- use emerald, mint, or neutral depending on meaning

Avoid:
- too many badge colors
- visually noisy badge collections
- large badges that compete with titles

---

## 13.14 Maps and Location Previews

Map modules should be muted and supportive, not visually dominant.

### Rules
- use grayscale or low-contrast map styling
- emerald pin or marker should be the focal point
- location details should sit directly below or beside the map
- include “Open in Maps” as a utility action, not as a dominant CTA

---

## 14. Screen-Specific Direction

## 14.1 Explore

This is the discovery hub.

### Priorities
1. search
2. filters
3. trending or recommended events
4. card scanning
5. easy join action

### Feel
- open
- fast to scan
- social
- motivating without being loud

### Composition
- left filter rail
- centered hero headline and search
- chip row under search
- event grid below

---

## 14.2 My Events

This screen should feel calmer and more operational.

### Priorities
1. upcoming bookings
2. quick ticket access
3. cancel / manage
4. history and rebooking

### Feel
- organized
- reassuring
- lightly premium
- less promotional than Explore

---

## 14.3 Create Event

This is a host-oriented screen and should feel structured and trustworthy.

### Priorities
1. form completion
2. clarity
3. confidence in publishing
4. quick understanding of event setup

### Feel
- guided
- deliberate
- lightweight
- premium SaaS + club management hybrid

---

## 14.4 Event Detail

This screen should combine emotion and conversion.

### Priorities
1. hero image and event identity
2. essential details
3. booking panel
4. host trust
5. nearby recommendations

### Feel
- immersive
- premium
- trustworthy
- easy to commit from

---

## 15. Motion

Animation should be subtle and supportive.

### Use motion for
- hover feedback
- chip selection
- card elevation
- page transitions between marketplace flows
- sticky panel appearance
- collapsible filters on smaller screens

### Avoid
- bouncy motion
- overscaled hover states
- flashy transitions
- sports-app gimmicks

Preferred motion language:
- short
- smooth
- restrained
- confident

---

## 16. Accessibility

This design system should remain premium without sacrificing usability.

### Requirements
- maintain strong text contrast on light surfaces
- ensure primary CTAs remain clearly legible
- provide visible keyboard focus states
- make icon-only actions large enough to tap comfortably
- do not rely on color alone for important status communication
- ensure selected states are visible through both color and shape/emphasis
- keep body text readable and avoid overly light gray copy

### Focus states
Focus should feel designed, not default-browser accidental:
- emerald focus ring or clear outline
- consistent across buttons, inputs, chips, and icon buttons

---

## 17. Implementation Notes

### Tokens
Design should be tokenized for:
- color
- spacing
- radius
- shadow
- typography
- component states

### Suggested naming
- `color-primary`
- `color-primary-foreground`
- `color-surface`
- `color-surface-muted`
- `color-border-subtle`
- `radius-sm`
- `radius-md`
- `radius-lg`
- `shadow-soft`
- `text-display`
- `text-title`
- `text-body`
- `text-label`

### Component mindset
Build reusable primitives for:
- button
- chip
- input
- card
- badge
- section header
- side panel
- booking summary
- event metadata row

---

## 18. Do / Don’t

### Do
- use emerald as the main interaction color
- keep backgrounds light and breathable
- use generous spacing
- let photography carry emotion
- make booking and hosting flows feel reliable
- use cards to structure dense information
- keep iconography consistent and restrained

### Don’t
- don’t make the app look like a gym dashboard
- don’t overuse borders or dividers
- don’t crowd cards with too many actions
- don’t use too many accent colors
- don’t let secondary metadata overpower titles and CTAs
- don’t make the experience feel sterile or overly SaaS-like
- don’t use loud gradients, neon greens, or heavy shadows

---

## 19. One-Sentence Standard

If a new screen or component is added, it should look like it belongs to **a premium sports community platform built around clean discovery, trusted booking, and elegant club-like organization**.
