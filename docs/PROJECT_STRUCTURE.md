# SFU Marketplace — Project Structure

**CMPT 276 · Group 10**  
**Status:** Draft for team review (October 30, 2026)

This document is a starting agreement for how we build the project together. We can change it at our meeting, but after approval we should use the same names and structure.

## 1. Technology

- **Backend:** Java 21, Spring Boot 4.1.1, Maven
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript, Bootstrap
- **Database:** PostgreSQL with Spring Data JPA
- **Authentication:** Spring Security with our own SFU email/password registration and login pages
- **Deployment:** Docker on Render, from the `main` branch
- **GitHub:** One shared repository: https://github.com/drippinmuh/Group-10

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

These are the agreed **proposed names and fields**, not classes that are already implemented. The person who owns the model can add extra fields later after telling the team.

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
| GET | `/listings` | Browse/search listings | Oleh + Jashith |
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

| Member | Main responsibility | Shared dependency |
| --- | --- | --- |
| **Ethan** | `UserAccount`, registration/login/logout, `Role`, account access | Everyone uses `UserAccount` |
| **Oleh** | `Listing`, listing pages, create/view listings | Search, messaging and reports use `Listing` |
| **Jashith** | Listing search/filters, favourites and ratings | Uses `Listing` and `UserAccount` |
| **Faraz** | Messaging, meetup planning and Nominatim integration | Uses `Listing` and `UserAccount` |
| **Maxim** | Safety/moderation, report handling, admin moderation pages | Uses `Listing`, `UserAccount` and `Role` |

**Shared database setup:** Maxim can prepare the first PostgreSQL connection and configuration, while Ethan and Oleh review the fields and relationships before it is merged. Database passwords and API keys must never be committed to GitHub. Use environment variables locally and on Render.

When changing a shared model, tell the affected teammates first. For example, if Oleh changes `Listing`, he should let Jashith, Faraz and Maxim know.

## 7. Iteration 1 Minimum (Due November 2, 2026)

The course requires a working application, not just empty pages. Our first usable feature will be **viewing and creating listings**.

1. **Authentication:** working SFU email/password registration, login and logout. The login flow must be implemented by the team, not replaced with Google sign-in. Passwords must be securely hashed.
2. **Roles:** `USER` and `ADMIN` can log in and see different landing pages. Admin-only pages must be protected, not just hidden from the navbar.
3. **Listings:** users can browse listings, open one listing, and logged-in users can create a listing. The data must still be there after the application restarts.
4. **Deployment:** everything above works on Render, not just localhost. Provide a working admin login to the instructor through the submission instructions, not in the public README.
5. **Team process:** make GitHub issues and use feature branches, PRs, reviews and comments so everyone's work is visible.
6. **Submission:** Requirements Document with consistent user stories (persona, action, result, successful and unsuccessful test cases), a TXT file with GitHub and Render links, and a short screencast showing the Render address bar.

Nice-to-have features, such as favourites, messaging, ratings, reports, email confirmation and the map API, should not block the Iteration 1 minimum. We can add them in later iterations.

## 8. Git Workflow

Follow [CONTRIBUTING.md](../CONTRIBUTING.md): separate branches, small PRs, at least one review and tests before merging to `main`. The `main` branch is the Render deployment branch. Avoid simultaneous edits of shared models without talking to the owner.

## 9. Meeting Approval (October 30)

We should review this document together and agree on:

- The stack, naming rules, main models and URLs.
- Who sets up PostgreSQL, Security and the first admin account.
- Iteration 1 minimum feature: login + roles + basic listings.
- GitHub issues and deadlines for each person.

Once everyone agrees, we can treat this document as our shared starting plan and update it whenever we make a team decision.
