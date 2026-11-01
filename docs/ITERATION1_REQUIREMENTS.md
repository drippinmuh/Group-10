
> **Status: DRAFT — November 1, 2026**
>
> This document is a working draft for Iteration 1.
> The Admin Dashboard requirements are based on Maxim's provided user story and acceptance tests.
>
> Authentication and Listings requirements are preliminary and awaiting confirmation from Ethan and Oleh.
>
> Acceptance tests describe expected behaviour and have not all been verified against the deployed application.

# SFU Marketplace — Iteration 1 Requirements Document

**Course:** CMPT 276 — Introduction to Software Engineering  
**Group:** Group 10  
**Prepared by:** Jashith Garg  
**Date:** November 1, 2026  
**Version:** 1.0 — Draft for Team Review  

**GitHub:** https://github.com/drippinmuh/Group-10  
**Render:** https://group-10-2hy5.onrender.com/

---

## 1. Introduction

### 1.1 Project Overview

SFU Marketplace is a web application designed to help Simon Fraser University students buy and sell second-hand items, including textbooks, electronics, furniture, and other everyday products.

Our goal is to provide a convenient and accessible marketplace specifically for the SFU community.

### 1.2 Purpose of This Document

This document defines the functional requirements and user stories for Iteration 1 of SFU Marketplace.

Each user story follows the format:

**As a [type of user], I want [an action or feature], so that [a benefit or result].**

Each story also includes acceptance criteria and test cases covering successful and unsuccessful scenarios.

The tests describe expected behaviour and must be verified after feature integration.

### 1.3 Iteration 1 Scope

Iteration 1 focuses on delivering a basic but functional marketplace.

The main features are:

1. User registration using SFU email addresses.
2. User login and logout.
3. Separate views for standard users and administrators.
4. An administrator dashboard.
5. Browsing marketplace listings.
6. Viewing individual listing details.
7. Creating product listings.
8. Persistent database storage using PostgreSQL.

Advanced search, filters, favourites, ratings, messaging, and meetup coordination are planned for future iterations.

---

## 2. User Types

| User Type | Description | Permissions |
|---|---|---|
| Guest | Visitor who has not logged in | Browse public pages, view listings, register, and log in |
| Registered User (USER) | Student with an account | Log in, log out, browse listings, and create listings |
| Administrator (ADMIN) | Authorized marketplace administrator | Access administrator dashboard and authorized administrative features |

Standard users must not be allowed to access administrator-only functionality.

---

## 3. User Stories and Acceptance Tests

### US-01: User Registration

**Priority:** High  
**Assigned Developer:** Ethan Cratchley  
**Epic:** User Accounts and Verification

**User Story:**

As an SFU student, I want to register for an account using my SFU email address so that I can access the marketplace and create listings.

**Acceptance Criteria:**

- Users can access the registration page.
- Users can provide the required registration information.
- Valid SFU email addresses are accepted.
- Duplicate accounts cannot be registered using the same email.
- Passwords are stored securely.
- New users receive the USER role.
- Users cannot register themselves as administrators.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| REG-01 | Success | Register using valid details and an SFU email address | Account is created with USER role |
| REG-02 | Failure | Attempt registration using a non-SFU email | Registration is rejected |
| REG-03 | Failure | Register using an email that already exists | An error is displayed and no duplicate account is created |
| REG-04 | Failure | Submit the form with missing required fields | Validation errors are displayed |
| REG-05 | Failure | Attempt to obtain ADMIN privileges through registration | Administrator access is not granted |

---

### US-02: User Login

**Priority:** High  
**Assigned Developer:** Ethan Cratchley  
**Epic:** User Accounts and Verification

**User Story:**

As a registered SFU student, I want to log in using my email and password so that I can securely access marketplace features.

**Acceptance Criteria:**

- Registered users can log in with valid credentials.
- Successful login creates an authenticated session.
- Invalid credentials do not grant access.
- Appropriate error messages appear when authentication fails.
- Users are directed to an appropriate view after login.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| LOG-01 | Success | Enter valid email and password | User logs in successfully |
| LOG-02 | Failure | Enter correct email and incorrect password | Login is rejected |
| LOG-03 | Failure | Attempt login using an unregistered email | Authentication fails |
| LOG-04 | Failure | Submit empty login fields | Login is not permitted |

---

### US-03: User Logout

**Priority:** High  
**Assigned Developer:** Ethan Cratchley  
**Epic:** User Accounts and Verification

**User Story:**

As a logged-in user, I want to log out of my account so that I can safely end my session.

**Acceptance Criteria:**

- Authenticated users can log out.
- Logging out terminates the authenticated session.
- The user returns to an appropriate public page.
- Protected functionality cannot be accessed using the previous session.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| OUT-01 | Success | Click Logout while authenticated | Session ends successfully |
| OUT-02 | Failure/Security | Attempt to create a listing after logging out | Authentication is required |
| OUT-03 | Failure/Security | Access the admin dashboard after logging out | Access is blocked or redirected to login |

---

### US-04: Role-Based User Views

**Priority:** High  
**Assigned Developer:** Ethan Cratchley  
**Epic:** User Accounts and Verification

**User Story:**

As a registered marketplace user, I want to see an interface appropriate to my role so that I can easily access the features available to me.

**Acceptance Criteria:**

- Standard users can access marketplace functionality.
- Administrators have a separate dashboard.
- Administrator-only actions are restricted.
- Access control is enforced by the application, not only through hidden UI elements.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| ROLE-01 | Success | Log in with a standard USER account | Standard user view is displayed |
| ROLE-02 | Success | Access listings while logged in as USER | Listings are accessible |
| ROLE-03 | Failure | USER attempts to access `/admin` | Access is denied |
| ROLE-04 | Failure | USER attempts an administrator-only action | Action is blocked |

---

### US-05: Administrator Dashboard

**Priority:** High  
**Assigned Developers:** Maxim Sarukhanov and Ethan Cratchley  
**Epic:** Safety and Moderation

**User Story:**

As a marketplace administrator, I want to access a separate admin dashboard so that I can manage the marketplace and access moderation features.

**Acceptance Criteria:**

- Administrators can access `/admin`.
- The dashboard displays available navigation and sections.
- Standard users cannot access the dashboard.
- Unauthenticated visitors must log in before accessing the dashboard.
- The dashboard provides a foundation for moderation features in future iterations.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| ADM-01 | Success | ADMIN logs in and accesses `/admin` | Admin dashboard is displayed |
| ADM-02 | Failure | Regular USER attempts to access `/admin` | Access is denied |
| ADM-03 | Failure | Unauthenticated visitor accesses `/admin` | Visitor is redirected to login |
| ADM-04 | Success | ADMIN opens the dashboard | Navigation and available sections are displayed |

**Implementation Progress:**

Maxim has created the initial administrator dashboard and an automated test.

He has also configured PostgreSQL on Render and connected it to the Spring Boot application.

The database connection is working.

However, the final dashboard access restrictions still require integration with Ethan's authentication implementation.

The requirements and acceptance tests above were provided by Maxim and should be rechecked after integration.

---

### US-06: Browse Marketplace Listings

**Priority:** High  
**Assigned Developer:** Oleh Kutryk  
**Epic:** Creating and Managing Listings

**User Story:**

As an SFU student, I want to browse available product listings so that I can discover affordable second-hand items sold by other students.

**Acceptance Criteria:**

- Visitors can access the listings page.
- Available listings are retrieved from the database.
- Listings display relevant information such as title and price.
- Visitors can open individual listings.
- The application handles an empty marketplace appropriately.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| LIST-01 | Success | Open the listings page when items exist | Available listings are displayed |
| LIST-02 | Success | Browse listings without logging in | Public listings remain accessible |
| LIST-03 | Edge Case | Open the listings page when no items exist | An appropriate empty-state message appears |
| LIST-04 | Success | Select an existing listing | Listing details are displayed |

---

### US-07: View Listing Details

**Priority:** High  
**Assigned Developer:** Oleh Kutryk  
**Epic:** Creating and Managing Listings

**User Story:**

As a potential buyer, I want to view the details of a product listing so that I can determine whether the item meets my needs.

**Acceptance Criteria:**

- Users can select a listing to view its details.
- Details include the title, description, price, and available seller information.
- An item image may be displayed if provided.
- Invalid or nonexistent listing identifiers are handled appropriately.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| DETAIL-01 | Success | Open an existing listing | Correct item details are displayed |
| DETAIL-02 | Success | Open a free listing with a price of $0 | Item displays the correct price |
| DETAIL-03 | Failure | Open a nonexistent listing | An appropriate not-found response appears |
| DETAIL-04 | Failure | Provide an invalid listing ID | Application handles the error gracefully |

---

### US-08: Create a Marketplace Listing

**Priority:** High — Main Marketplace Feature  
**Assigned Developer:** Oleh Kutryk  
**Epic:** Creating and Managing Listings

**User Story:**

As a registered SFU student, I want to create a listing for an item I no longer need so that other students can discover and purchase it.

**Acceptance Criteria:**

- Authenticated users can access the listing creation form.
- Users can provide a title, description, and price.
- Additional fields may be available depending on the final implementation.
- A listing must have a valid title and description.
- Prices must be nonnegative.
- Listings can be associated with the authenticated seller.
- Unauthenticated users cannot create listings.
- Successfully created listings are stored in the database.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| CREATE-01 | Success | Create a listing for a textbook priced at $25 | Listing is saved and becomes visible |
| CREATE-02 | Success | Create a free listing priced at $0 | Listing is accepted |
| CREATE-03 | Failure | Submit a listing without a title | Validation error is displayed |
| CREATE-04 | Failure | Submit a listing without a description | Submission is rejected |
| CREATE-05 | Failure | Enter a negative price | Invalid price is rejected |
| CREATE-06 | Failure | Attempt listing creation without logging in | Authentication is required |

---

### US-09: Persistent Listing Storage

**Priority:** High  
**Assigned Developers:** Oleh Kutryk and Maxim Sarukhanov  
**Epic:** Creating and Managing Listings

**User Story:**

As a marketplace seller, I want my listings to remain saved after I leave the website so that other students can continue viewing my items.

**Acceptance Criteria:**

- Valid listings are saved in PostgreSQL.
- Listings remain available after refreshing the page.
- Listings can be retrieved in later sessions.
- Invalid listing submissions are not stored.
- Database-backed records remain available after application restarts.

**Acceptance Tests:**

| Test ID | Type | Scenario | Expected Result |
|---|---|---|---|
| DATA-01 | Success | Create a listing and refresh the page | Listing remains available |
| DATA-02 | Success | Create a listing, log out, and return | Listing remains stored |
| DATA-03 | Success | Restart the application and retrieve a saved listing | Previously stored listing is retrieved |
| DATA-04 | Failure | Submit invalid listing information | Invalid listing is not saved |

---

## 4. Non-Functional Requirements

### 4.1 Security

- Passwords must be stored securely rather than in plaintext.
- Authentication must be enforced using Spring Security.
- Administrator functionality must be restricted to authorized accounts.
- Unauthorized users must not be permitted to perform protected operations.
- Sensitive configuration information must not be committed to GitHub.

### 4.2 Usability

- The website should provide consistent navigation.
- Form errors should be communicated clearly.
- Public and authenticated pages should be easy to distinguish.
- The interface should be accessible through a modern web browser.

### 4.3 Data Integrity

- Registered email addresses must be unique.
- Listings must have valid required fields.
- Prices must be nonnegative.
- Listings should be associated with their creators.
- Invalid submissions must not create invalid database records.

### 4.4 Deployment

- The application must be accessible through Render.
- User accounts and listings should be stored persistently in PostgreSQL.
- The required functionality should be tested on the deployed application.

---

## 5. UI Mockups and Page Layouts

The following tables represent conceptual UI mockups. They describe intended layouts rather than confirmed screenshots of the completed application.

### Mockup 1: Login Page

| Component | Expected Interface |
|---|---|
| Navigation | Home, Listings, Login, Register |
| Heading | Login to SFU Marketplace |
| Input | Email Address |
| Input | Password |
| Button | Login |
| Link | Register |
| Feedback | Error message for invalid credentials |

**Related Stories:** US-01, US-02

### Mockup 2: Marketplace Listings

| Component | Expected Interface |
|---|---|
| Navigation | Home, Listings, Logout |
| Heading | Marketplace Listings |
| Content | Available products |
| Example | CMPT 276 Textbook — $25 |
| Example | Desk Lamp — $10 |
| Button | Create Listing |
| Action | View Listing Details |

**Related Stories:** US-04, US-06, US-07, US-08

### Mockup 3: Create Listing

| Component | Expected Interface |
|---|---|
| Heading | Create Listing |
| Input | Item Title |
| Input | Description |
| Input | Price |
| Optional Inputs | Category, Condition, Campus, Image URL |
| Button | Create Listing |
| Feedback | Validation errors or success message |

**Related Stories:** US-08, US-09

### Mockup 4: Administrator Dashboard

| Component | Expected Interface |
|---|---|
| Heading | Administrator Dashboard |
| Navigation | Dashboard, Logout |
| Main Content | Available administration sections |
| Future Features | Listing moderation and reports |
| Access | ADMIN accounts only |

**Related Story:** US-05

These mockups should be reviewed against the final application interface before submission.

---

## 6. Development Responsibilities

The Iteration 1 workload is distributed across the five group members.

| Team Member | Iteration 1 Responsibility |
|---|---|
| Ethan Cratchley | Registration, login/logout, user roles, and Spring Security |
| Oleh Kutryk | Listing model, creation, browsing, details, and persistence |
| Maxim Sarukhanov | PostgreSQL setup, admin dashboard, deployment, and integration |
| Jashith Garg | Requirements Document, user stories, acceptance tests, and documentation |
| Faraz Seyfi | Screencast demonstrating the deployed application |

The team uses GitHub Issues, branches, commits, pull requests, and Discord to manage development and communication.

**Relevant GitHub Issues:**

- Issue #2 — Authentication and User Roles
- Issue #3 — Basic Listings
- Issue #5 — Admin Dashboard
- Issue #6 — Requirements Document
- Issue #7 — Screencast
- Issue #8 — Integration and Final Submission

---

## 7. Testing and Verification Plan

The acceptance tests in Section 3 describe the expected behaviour for each user story.

They must be verified against the integrated application before being marked as passed.

The team should complete the following checks:

1. Verify successful and unsuccessful registration.
2. Test login with correct and incorrect credentials.
3. Confirm that logout terminates the session.
4. Verify that USER accounts cannot access `/admin`.
5. Verify that ADMIN accounts can access the dashboard.
6. Test listing browsing and detail pages.
7. Test valid and invalid listing submissions.
8. Confirm database persistence.
9. Repeat the main workflows on Render.

The final test results should be reviewed with the developers responsible for each feature.

### Current Verification Status

| Feature | Current Status |
|---|---|
| PostgreSQL connection | Reported working by Maxim |
| Admin dashboard interface | Initial implementation reported by Maxim |
| Admin security integration | Awaiting final integration confirmation |
| Registration and login/logout | Awaiting Ethan's verification |
| Listings | Awaiting Oleh's verification |
| Full application testing | Pending team integration |

---

## 8. Future Iterations

The following features are part of the larger SFU Marketplace project but are outside the main requirements for Iteration 1.

### Search, Filters, Ratings, and Favourites

**Assigned Developer:** Jashith Garg

Students will be able to search listings by keywords and apply filters such as price, condition, category, campus, and course code.

Users will eventually be able to save favourite items and leave seller ratings.

### Messaging and Meetup Coordination

**Assigned Developer:** Faraz Seyfi

Students will be able to communicate with buyers and sellers, ask questions, and arrange item pickups.

### Safety and Moderation

**Assigned Developer:** Maxim Sarukhanov

Administrators will eventually have tools to review suspicious listings, handle reports, and remove content violating marketplace rules.

### Account Verification Improvements

**Assigned Developer:** Ethan Cratchley

Future improvements may include email verification and password recovery.

---

## 9. Conclusion

Iteration 1 establishes the foundation for SFU Marketplace by focusing on account management, secure login/logout, role-based access, administrator functionality, and basic product listings.

The user stories and acceptance tests in this document provide a consistent description of the expected system behaviour.

Following the integration of all features, the team will review the acceptance tests against the deployed application and update any requirements that differ from the implementation.

This document will serve as a foundation for future iterations, including advanced search, favourites, ratings, messaging, and moderation functionality.
