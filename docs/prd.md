# Product Requirements Document (PRD)
## Event Community Platform (Placeholder Name)

**Document version:** 1.1  
**Status:** Active  
**Product type:** Web application  
**Primary audience:** Product/design/development teams and AI coding assistants  
**Language:** English  

**Current scope note:** The repository is no longer limited to the MVP. References to the MVP below are kept as historical phase context and should not be used to block full-scope work such as authentication, authorization, reviews, or moderation.

---

## 1. Product Overview

### 1.1 Summary
Event Community Platform is a web application designed to 
help people discover, join, and organize in-person matches more easily. The core problem it addresses is that many people struggle to regularly find activities, sports groups, classes, tournaments, or similar matches unless they discover them by chance. The focus is on sporting matches. 

The platform supports two modes within the same account:
- **User mode**, focused on discovering and joining matches
- **Host mode**, focused on creating and managing matches

A single person can act as both a participant and a host, similar to the role-switching model used in platforms like Airbnb.

### 1.2 Core Value Proposition
The platform makes it easier for people to:
- Discover relevant matches in one place
- Join activities and communities without relying on chance
- Organize and publish matches for others
- Build trust and reputation through reviews

### 1.3 Product Positioning
The product should feel social, community-oriented, and approachable rather than overly corporate, transactional or modern.

---

## 2. Problem Statement

Today, finding matches, activity groups, classes, or tournaments is fragmented and inconsistent. People often depend on chance, word of mouth, social media, or scattered groups to find opportunities to participate.

This creates friction for users who want to:
- Meet new people
- Find sports groups or activities
- Join classes, workshops, or tournaments
- Discover matches that match their interests

At the same time, people who organize matches lack a simple, centralized way to publish and manage them for a broader audience.

---

## 3. Product Goals

### 3.1 Goals for the Current Product Phase
- Enable users to discover in-person matches through a centralized web platform
- Enable users to reserve a spot in matches quickly and easily
- Enable hosts to create, edit, and delete matches
- Support a dual-role experience where the same account can act as both user and host
- Support account registration plus login/logout with email and password
- Support role-based access with a regular user level and an elevated admin/mod level

### 3.2 Historical MVP Non-Goals
The following were intentionally out of scope for the initial MVP, but they are no longer automatic blockers for current work:
- Enable hosts to edit matches
- Interactive maps
- Personalized recommendations
- Reviews
- Host verification
- In-app chat
- Notifications
- Waitlists
- Event moderation/reporting systems
- Real check-in/attendance validation

---

## 4. Target Users

### 4.1 General Participants
People who want to:
- Find matches that interest them
- Join sports, classes, workshops, tournaments, and social activities
- Explore matches by category or search terms
- Reserve a spot and manage their upcoming participation

### 4.2 Hosts
People who want to:
- Publish matches
- Share event details with potential attendees
- Manage their event listings
- See confirmed attendees
- Receive reviews after matches

### 4.3 Audience Scope
The platform is intended for an **open community**, not a closed university-only or invite-only audience.

---

## 5. User Roles and Account Model

### 5.1 Single Account, Dual Mode
The system uses **one account per person**. The same account can operate in:
- **User mode**
- **Host mode**

Switching between modes is **manual** and clearly visible in the interface, similar to Airbnb's role-switching pattern.

### 5.2 User Mode
In User mode, the person can:
- Browse event feed
- Search matches by free text
- Filter matches
- View event details
- Reserve a spot in an event
- Cancel their reservation
- View their own profile
- View their "My Events" section
- Leave a review after attending an event 

### 5.3 Host Mode
In Host mode, the person can:
- Create matches
- Edit and delete published matches
- View hosted matches
- View confirmed attendees for each event
- Manage event information
- Receive reviews associated with their host profile 

### 5.4 Authorization Levels
User mode and Host mode are product experiences within the same account. They are not the same thing as security roles.

The authorization model should support at least:
- **Regular user**, who can manage their own profile, reservations, and hosted content
- **Admin/mod**, an elevated role with near-full platform access used to satisfy role-based-access requirements and moderation needs

The elevated admin/mod role can:
- Edit or delete any event
- Intervene in moderation/reporting flows
- Access management screens that are not limited to a single event owner

---

## 6. Event Scope

### 6.1 Category Rules
- Each event belongs to **exactly one** category
- Categories are fixed predefined values for the initial product scope

### 6.3 Modality Rules
For the current scope:
- Events are **in-person only**
- Virtual matches are out of scope

### 6.4 Pricing Rules
- Events may be **free** or **paid**
- The platform does **not** process payments
- If payment is needed, it is informational only and handled outside the platform

---

## 7. Key Product Experience

### 7.1 Discovery Experience
The main landing experience for users is a **feed of matches**, not a map.

Users should be able to:
- Browse a feed of available matches
- Search by free text
- Filter by relevant criteria
- Sort results using different ordering methods

### 7.2 Search
The search experience must support **free-text search**. Example searches:
- "football 5"
- "tennis doubles"

### 7.3 Filters
Important filters include:
- Date
- Category
- Paid/Free
- Location
- Level required (Begginer, intermediate, advanced)

Other filters may be added later, but these are the minimum explicitly defined filters.

### 7.4 Sorting
Events should support multiple sorting methods. Sorting is not fixed to a single default business rule. Possible sorting options include:
- Nearest / closest
- Upcoming soonest
- Popularity
- Other future criteria

For implementation purposes, the UI should be built to support multiple selectable sort options.

---

## 8. Event Data Model Requirements

Each event should contain, at minimum, the following information:

- Title
- Description
- Category
- Location
- Date and time
- Duration
- Requirements
- Price
- Capacity
- One or more images
- Host reference
- Event status

### 8.1 Event Status
At minimum, the event lifecycle should support:
- Published
- Cancelled
- On going
- Completed

---

## 9. Repeating Events

The product must support matches with **multiple dates**.

The preferred model is:
- A single event concept
- Multiple scheduled dates/occurrences associated with that event

This means the system should not conceptually treat repeating matches as entirely unrelated cloned matches, even if implementation details later simplify the model.

This requirement matters for:
- Event display
- Reservation logic
- Capacity management
- Historical participation
- Reviews after attendance

---

## 10. Reservation / Enrollment Model

### 10.1 Reservation Meaning
"Signing up" means **reserving a spot** and confirming attendance in the event.

### 10.2 Reservation Behavior
- Reservation is **not instant**
- Users must confirm their inscription via mail
- Host can kick out players from matches
- A user can reserve **only once** per event
- If the event has started already, reservation is blocked automatically
- If the event reaches its capacity, no more reservations are allowed
- Waitlists are out of scope for the MVP

### 10.3 Cancellation Behavior
- A user can cancel their reservation
- When a reservation is cancelled, the spot is released automatically

### 10.4 Host Participation
- A host **can** reserve a spot in their own event

### 10.5 No Check-In Flow
- The system does not manage real attendance validation or check-in

---

## 11. Profile Experience

### 11.1 Public Profile
Each user should have a basic public profile visible to others.

Suggested visible profile information:
- Name
- Profile picture
- Bio
- Basic public identity information

This public profile may later be expanded with:
- Hosted matches
- Reviews received as a host
- Reputation indicators

### 11.2 Authentication
The system should support **traditional login only** for now:
- Email
- Password

Social login providers such as Google are out of scope.

---

## 12. My Events Experience

In User mode, users should have access to a **My Events** section containing:
- Upcoming Events
- Past Events

Hosted matches do **not** belong inside this screen, because hosted-event management belongs to Host mode.

---

## 13. Host Dashboard Experience

In Host mode, the interface and available actions should change meaningfully.

The host-facing experience should allow:
- Create event
- Edit event
- View hosted matches
- View confirmed attendees per event
- Manage event details

This is not just a visual mode toggle; it implies different workflows and available actions.

---

## 14. Reviews

### 14.1 Review Scope
Reviews are part of the broader product scope and may be implemented when prioritized.

### 14.2 Review Rules
When reviews are implemented:
- Only users who attended an event can leave a review to a host
- Each user can leave **only one review per host** 
- A user can leave a review to a host only if it attended to an event created by that host
- A review contains:
  - Star rating
  - Text review
- Reviews are associated with the **host**, not directly with the event
- Users can edit or delete their own reviews
- Host replies are out of scope

### 14.3 Moderation
Review moderation/reporting is part of the broader product scope and should be handled through the elevated admin/mod role.

---

## 15. Functional Requirements

### 15.1 Authentication
The system must allow users to:
- Register an account
- Log in with email and password
- Log out
- Access protected features according to their authorization level

### 15.2 User Discovery
The system must allow users to:
- View the event feed
- Search by free text
- Filter matches by date and category
- Sort event results
- View event details

### 15.3 Reservation
The system must allow users to:
- Reserve a spot in an event
- Cancel their reservation
- View upcoming and past reservations in My Events

### 15.4 Host Event Management
The system must allow hosts to:
- Switch into Host mode
- Create an event
- Edit an existing published event
- See their hosted matches
- See confirmed attendees for each hosted event

### 15.5 Public Profiles
The system must allow viewing basic public user profiles.

### 15.6 Administration And Moderation
The system must support an elevated admin/mod role that can:
- Edit or delete any event
- Access platform-level management actions that are not limited to the event owner
- Handle moderation/reporting workflows when those flows are implemented

---

## 16. Historical MVP Baseline

This section describes the original MVP boundary for reference only. It is not the current delivery ceiling for the repository.

### 16.1 Included in MVP
The MVP includes:
- Manual switching between User mode and Host mode
- Event feed
- Free-text search
- Category/date filtering
- Event detail view
- Event creation by hosts
- Reservation flow
- Cancellation flow
- Capacity handling
- Attendee list

### 16.2 Excluded from MVP
The MVP excludes:
- Login
- Public profiles
- Reviews
- Maps
- Recommendations
- Host verification
- Notifications
- Chat
- Waitlists
- Reporting/moderation systems
- Virtual matches
- Check-in flow

---

## 17. Business Rules Summary

- One account can act as both user and host
- Mode switching is manual
- Events are in-person only
- Each event belongs to one category
- Events may be free or paid
- Payments are not processed in-platform
- A user can reserve only once per event
- Reservation is instant
- Reservation closes once the event has started
- Cancelling a reservation releases the spot automatically
- Hosts can reject participants
- Hosts can reserve their own matches
- Repeating matches are modeled as one event with multiple dates
- Reviews are tied to hosts and limited to one per user per attended event

---

## 18. Open Questions / Product Decisions Still Flexible

The following items are not fully closed and may be refined later without breaking the current PRD structure:

1. Exact default sort order in the event feed
2. Exact event editing constraints close to event start time
3. Detailed behavior for cancelled/reprogrammed repeating occurrences
4. Exact structure of location data
6. Whether host-specific public profile fields should be richer than standard user profiles

These do not block product definition at the current stage.

---
