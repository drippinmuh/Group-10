# Authentication & Authorization — Notes (Iteration 1)

## What was implemented

- **`UserAccount` model** (table `users`) with `Role` enum (`USER`/`ADMIN`), as agreed in `PROJECT_STRUCTURE.md`.
- **Registration** (`POST /register`): requires full name, an `@sfu.ca` email, and a password of 8+ characters. Emails are lowercased and must be unique. Passwords are hashed with BCrypt — never stored in plain text. Registration always creates a `USER`.
- **Login/logout**: Spring Security form login with sessions. After login, `ADMIN` lands on `/admin`, `USER` on `/`. The navbar changes with login state (admins also see an Admin Dashboard link).
- **Access control**: `/admin` and `/admin/**` are restricted to `ADMIN` server-side. Creating a listing requires login. Home, listings, login, and register stay public. CSRF protection is on.
- **Admin accounts**: not creatable through registration. One is seeded at startup from the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables.

## Integration Notes

- Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` on Render; put the credentials in the submission TXT, not the repo.
- Until `feature/admin-dashboard` is merged, admin login redirects to `/admin` and 404s (security passes; the page just isn't on this branch). The merge resolves it; your tests are unaffected.

## Test cases

All automated: `./mvnw test` — 29 tests, all passing.

### Success

| Scenario                                               | Expected result                                                 |
| ------------------------------------------------------ | --------------------------------------------------------------- |
| Register with name, `name@sfu.ca`, password 8+ chars   | Account created as `USER`, password hashed, redirected to login |
| Register with ` Ethan@SFU.ca`                          | Stored as `ethan@sfu.ca`                                        |
| Log in as USER / as ADMIN                              | Authenticated; USER → `/`, ADMIN → `/admin`                     |
| Log out                                                | Session ended, back to login page                               |
| Visit home, listings, login, register while logged out | Pages load publicly                                             |
| Start app with `ADMIN_EMAIL`/`ADMIN_PASSWORD` set      | Admin account exists with `ADMIN` role                          |

### Failure

| Scenario                                                       | Expected result                    |
| -------------------------------------------------------------- | ---------------------------------- |
| Register with non-SFU email, or `@sfu.ca` with empty name part | Rejected with error, nothing saved |
| Register with an email already in use (any casing)             | Rejected, no duplicate             |
| Register with password under 8 chars or blank name             | Rejected, nothing saved            |
| Log in with wrong password or unknown email                    | Not authenticated, error shown     |
| Open `/admin` while logged out                                 | Redirected to login                |
| Open `/admin` or `/admin/reports` as USER                      | 403 Forbidden                      |
| POST `/register` without CSRF token                            | 403 Forbidden                      |
