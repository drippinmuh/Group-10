# SFU Marketplace — Project Structure

**CMPT 276 · Group 10**  
**Status:** Active — team working baseline (October 29, 2026)

This is our current shared structure for developing SFU Marketplace. New work should follow the same naming rules, models, and routes so our features fit together. We can update this document if the team agrees on a change. The Iteration 1 task split reflects our planning meeting; members who were absent are being contacted to confirm their assignments.

## 1. Technology

- **Backend:** Java 21, Spring Boot 4.1.1, Maven
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript, Bootstrap
- **Database:** PostgreSQL with Spring Data JPA
- **Authentication:** Spring Security with our own SFU email/password registration and login pages
- **Deployment:** Docker on Render, from the `main` branch
- **GitHub:** https://github.com/drippinmuh/Group-10

We will keep this as one Spring Boot application, not a separate frontend and backend. The four basic pages (`/`, `/listings`, `/login`, `/register`) and shared navbar are already in the project. The login and registration forms are not functional yet.

## 2. Naming Rules

| Item | Rule | Example |
| --- | --- | --- |
| Java classes | `PascalCase` | `ListingController`, `UserAccount` |
| Variables and methods | `camelCase` | `sellerId`, `findByEmail` |
| Constants and enum values | `UPPER_SNAKE_CASE` | `ADMIN`, `AVAILABLE` |
| Java packages | lowercase | `controller`, `model`, `service` |
| Database tables and columns | `snake_case` | `users`, `password_hash` |
| HTML templates | lowercase with hyphens | `listing-details.html` |
| Git branches | `feature/...`, `fix/...`, `docs/...` | `feature/listing-create` |

All database primary keys will use `Long`. Prices will use `BigDecimal` (not `double`), and a price of `0` means the item is free. We will use `Instant` for timestamps and store them in UTC.

## 3. Shared Models

These are our shared names and fields. They define what we intend to implement; they are not all classes already in the repository. The model owner should discuss changes to shared fields or types with affected teammates before changing them.

### `UserAccount` (database table: `users`)

| Field | Java type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `fullName` | `String` | Display name |
| `email` | `String` | Unique, lowercase, must end in `@sfu.ca` |
| `passwordHash` | `String` | Never store plain passwords |
| `role` | `Role` | `USER` or `ADMIN` |
| `createdAt` | `Instant` | Account creation time |

`Role` is an enum with `USER` and `ADMIN`. Every normal registration creates a `USER`. Admin accounts will be set up separately, not through the public registration form.

**Iteration 1:** Check that the email has the correct SFU domain. Actual email confirmation links can be added later; checking the domain alone is not proof that the user owns the email address.

### `Listing` (database table: `listings`)

| Field | Java type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `seller` | `UserAccount` | Many listings can belong to one user |
| `title` | `String` | Required |
| `description` | `String` | Required |
| `price` | `BigDecimal` | Required, 0 or more |
| `category` | `String` | e.g. Textbooks, Electronics |
| `condition` | `String` | e.g. New, Good, Fair |
| `campus` | `String` | e.g. Burnaby, Surrey, Vancouver |
| `imageUrl` | `String` | Optional for the first iteration |
| `status` | `ListingStatus` | `AVAILABLE`, `SOLD`, `REMOVED` |
| `createdAt` | `Instant` | Creation time |

Use a relationship to `UserAccount` for `seller`, not an unrelated `sellerId` field in the Java model. The `seller_id` foreign key can be created in the database.

### Later models (after the Iteration 1 minimum is working)

- **`Message`** (`messages`): `id`, `sender`, `receiver`, `listing`, `body`, `sentAt`.
- **`Review`** (`reviews`): `id`, `reviewer`, `reviewedUser`, `rating` (1–5), `comment`, `createdAt`. We need to agree later on how to confirm a completed trade.
- **`Report`** (`reports`): `id`, `reporter`, `listing`, `reason`, `status`, `createdAt`. `ReportStatus` will be `PENDING`, `RESOLVED`, or `DISMISSED`.

Please do not create a second `User`, `Listing`, or `Role` model in your own epic. Use the shared ones instead.

## 4. Routes and Pages

| Method | URL | Purpose | Owner |
| --- | --- | --- | --- |
| GET | `/` | Homepage | Shared |
| GET | `/login` | Login form | Ethan |
| POST | `/login` | Log in | Ethan |
| POST | `/logout` | Log out | Ethan |
| GET | `/register` | Registration form | Ethan |
| POST | `/register` | Create user account | Ethan |
| GET | `/listings` | Browse listings (search/filters in later iterations) | Oleh |
| GET | `/listings/{id}` | View one listing | Oleh |
| GET | `/listings/new` | New listing form | Oleh |
| POST | `/listings` | Save a new listing | Oleh |
| GET | `/admin` | Admin landing page | Ethan + Maxim |
| GET | `/admin/reports` | Moderation page (later) | Maxim |
| POST | `/reports` | Report a listing (later) | Maxim |
| GET | `/messages` | Messages (later) | Faraz |

Search and filters will use query parameters on `/listings`, for example `/listings?query=calculator&category=Electronics`, instead of a separate search URL.

Login and logout will use normal form submissions and server-side sessions, not Google login. `USER` and `ADMIN` must have different views after login. Only admins can access `/admin` pages. The real login system must be implemented for Iteration 1; the current HTML forms are placeholders.

## 5. Java and HTML Folders

```text
src/main/
├── java/ca/sfu/cmpt276/marketplace/
│   ├── MarketplaceApplication.java
│   ├── controller/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── config/
└── resources/
    ├── application.properties
    ├── templates/
    │   ├── index.html
    │   ├── listings.html
    │   ├── login.html
    │   ├── register.html
    │   └── fragments/navbar.html
    └── static/css/
```

- `controller`: URLs and page requests
- `model`: shared database entities and enums
- `repository`: Spring Data JPA database access
- `service`: business logic
- `config`: security or other shared configuration
- `templates`: Thymeleaf HTML
- `static`: CSS, JavaScript and other static files

The existing starter controllers are currently in the base package. They can stay there until someone needs to reorganize them; we do not need a big refactor before development starts.

## 6. Responsibilities

### Iteration 1 task split

| Member | Responsibility | Deliverable |
| --- | --- | --- |
| **Ethan** | Authentication and access control | Working registration, login, logout, `UserAccount`, `Role`, Spring Security, and USER/ADMIN access rules |
| **Oleh** | Basic listings | `Listing` model, create/view listings, and saving them in PostgreSQL |
| **Jashith (Drippin)** | Requirements Document | Consistent user stories, success and failure test cases, and any needed mockups, using input from the whole team |
| **Faraz** | Screencast | Record the working site on Render with the URL visible and demonstrate the required flows |
| **Maxim** | Shared infrastructure, admin page, and integration | PostgreSQL/Render configuration, `/admin` dashboard UI, coordinating integration, end-to-end testing, and remaining submission coordination |

This split is specifically for **Iteration 1**; it does not change the five epics in our project proposal.

### Longer-term epic ownership

- **Ethan — Epic 1:** User Accounts and Verification.
- **Oleh — Epic 2:** Creating and Managing Listings.
- **Jashith — Epic 3:** Search, Filters, Ratings, and Favourites.
- **Faraz — Epic 4:** Messaging and Meetup Coordination.
- **Maxim — Epic 5:** Safety and Moderation.

### Shared work and dependencies

- **Maxim** sets up the PostgreSQL connection, JPA dependency, and Render database environment configuration. **Ethan** owns the `UserAccount` entity, and **Oleh** owns the `Listing` entity. They coordinate their entity relationships instead of duplicating models.
- **Ethan** configures Spring Security to restrict admin URLs. **Maxim** implements the `/admin` page and coordinates its integration with Ethan's security setup.
- **Oleh** implements the basic listing routes and forms. Search and filters are part of Jashith's later epic, not required for the first working version.
- **Everyone** sends Jashith user stories and success/failure test cases for their features, and gives Faraz a working demo flow to record.
- Do not commit database passwords, tokens, or API keys. Use environment variables locally and on Render.
- Discuss changes to shared models or routes with affected teammates first.

## 7. Iteration 1 Plan (Due November 2, 2026)

The goal is a small, **working** marketplace on Render, not five unfinished epics.

### Working features

1. **Registration, login and logout (Ethan):** users can create accounts with an SFU email, log in and log out. Passwords are securely hashed. We implement our own login rather than replacing it with Google sign-in.
2. **User/admin views and access (Ethan + Maxim):** regular users and admins have different views. Ethan protects admin routes with Spring Security; Maxim builds the `/admin` page. Hiding links alone is not access control.
3. **Basic listings (Oleh):** visitors can browse listings and see listing details; logged-in users can create a listing. Data is stored in PostgreSQL and remains after a restart.
4. **Database and deployment (Maxim):** configure PostgreSQL and Render and verify the full flow on the deployed website, not only localhost.
5. **Team collaboration (everyone):** track work in GitHub issues, feature branches, commits, reviews and pull requests wherever possible.

### Documentation, recording and submission

- **Jashith — Requirements Document:** collect user stories from the team. Each story should consistently describe a user, desired action, and reason; include both successful and unsuccessful acceptance test cases. Add UI mockups where they help explain a requirement. The document describes intended behavior, not only what was coded.
- **Faraz — Screencast:** display the deployed Render URL in the browser address bar and demonstrate the features that work, including registration/login/logout, user/admin views and listings. Record after integration has been tested.
- **Maxim — Final integration and submission check:** make sure the deployed app and tests work, the Requirements Document and screencast are ready, and the submission TXT includes the GitHub URL, Render URL and working admin login details. Admin credentials should go through the course submission, **not** in the public repository.
- **Everyone:** send Jashith feature descriptions and acceptance test cases, keep track of progress in GitHub, and help test before Faraz records the screencast.

### Internal target dates

| Target | Goal |
| --- | --- |
| **October 30** | PostgreSQL setup, shared interfaces and task ownership coordinated |
| **October 31** | First functional Authentication and Listings versions; Requirements Document draft |
| **November 1** | Merge and test on Render, finalize documentation and record screencast |
| **November 2** | Final checks and submission |

These are internal targets to leave time for integration; the course deadline is **November 2**.

Email confirmation links, messaging, favourites, ratings, full report moderation, and the Nominatim meetup API belong to later iterations unless our required work is already complete.

## 8. Git Workflow

Follow [CONTRIBUTING.md](../CONTRIBUTING.md): separate branches, small PRs, at least one review and tests before merging to `main`. The `main` branch is the Render deployment branch. Avoid simultaneous edits of shared models without talking to the owner.

## 9. Working Agreements

- This document is the current project baseline. New contributions should follow the names, data types and routes described above.
- The team chose this structure during its Iteration 1 planning discussion on **October 29, 2026**. Ethan and Jashith have been contacted separately about their Iteration 1 assignments because not everyone attended the meeting.
- If a shared decision changes, discuss it with the affected members and update this document so implementation and documentation stay consistent.
- Our first priority is completing and deploying **Authentication + Roles + Basic Listings** before optional features.
