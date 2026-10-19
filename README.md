# Group 10

## Project Abstract

Sfu marketplace is a web application where students can buy & sell second-hand items like textbooks, electronics, furniture etc. Students will be able to create listings, search for items, mark favourites and contact sellers. Our goal is to make buying and selling more convenient for SFU students.

## Problem and Target Market

SFU students often have items they no longer need - such as textbooks, while other students may be looking for exactly those items but don’t want to pay full price for them. Sometimes students are even willing to give items away for free. However, it can be difficult to find other students to buy from or sell to, since there is no convenient marketplace for the SFU community.

Our target audience is SFU students, especially those looking to save money by buying second-hand items or selling things they no longer need. Our project will make it easier to find affordable items.

## Existing Solutions

[https://play.google.com/store/apps/details?id=com.yourcompany.dormmarketplace&hl=en_CA](https://play.google.com/store/apps/details?id=com.yourcompany.dormmarketplace&hl=en_CA) **(u-Shop Sfu) -** mobile app only available on google play,

[https://github.com/AmarKoonar/Sfu-Market](https://github.com/AmarKoonar/Sfu-Market) **(Sfu-Market) -** hackathon project saying in-progress, production builds fail, err 500

conclusion - research shows no current competitors

## Our Solution

Our solution is to create a simple and user-friendly website where SFU students can buy and sell used items. Unlike SFU’s u-Shop, which is a mobile app, our marketplace will run directly in a web-browser, so students won’t need to download anything.

Another similar project, SFU-Market, was created during a hackathon and is still listed on Github as “in development”. Our goal is to build a fully-fledged and reliable website that students can easily access and use.

We also plan to include useful features such as search filters, favourites, messaging, and meeting scheduling. We want to make buying and selling within the SFU community as simple and convenient as possible.

## Scope and Major Epics

SFU Marketplace is made up of several individual features that build around the central shared listing system rather than many individual features. We divided the project into five major epics, one per person, with each epic including several smaller tasks to be designed as tickets, implemented and tested.

### Epic 1: User Accounts and Verification — Ethan

Users will be able to register, log in, log out and manage a profile with their name, preferred campus and active listings. Registration will be restricted to SFU email addresses and new accounts will be confirmed through an email verification link. Passwords will be stored securely but also capable of being reset if you forget the password. This epic also defines user roles that the moderation tools in Epic 5 will utilize.

### Epic 2: Creating and Managing Listings - Oleg

Users will be able to view listings as a list, and view a selected listing. Listing should include title, description, picture, price, and some of the User-seller information. Users that are logged in (and verified) can create a new listing. Listing can be edited or deleted by its owner.

### Epic 3: Search, Filters, Ratings, and Favourites - Jashith

Users will be able to search and filter items by keyword, price, condition, category, campus location, and course code (e.g., *CMPT276*). Students can also save items to a personal Favourites list and view or leave seller ratings to build community trust.

### Epic 4: Messaging and Meetup Coordination - Faraz

Users will be able to send and receive messages to communicate with buyers and sellers. They can ask questions about items, discuss prices, and arrange a time and place to meet. We also plan to use a map API to help users find convenient meetup locations around SFU.

### Epic 5: Safety and Moderation - Maxim

To make a marketplace more reliable, users will be able to report suspicious or inappropriate listings. We plan to include basic moderation tools that allow administrators to review reports and remove listings that violate the platform’s rules.

## External Web APIs

[https://nominatim.org/release-docs/latest/api/Overview/](https://nominatim.org/release-docs/latest/api/Overview/)

We plan to use the OpenStreetMap Nominatim REST API to help students find meetup locations around SFU. The API allows us to search for places and receive their names and coordinates in JSON format. Our Spring Boot application can use this information to help buyers and sellers arrange pickups. We will follow the API's usage limits and cache results to avoid unnecessary requests.

## Technology and Initial Setup

For the frontend, we plan to use HTML, CSS, JavaScript, Thymeleaf, and Bootstrap. Thymeleaf will allow us to create dynamic web pages using Spring Boot, while Bootstrap will help us build a user-friendly interface.

For the database, we will use PostgreSQL to store user accounts, product listings, messages, favourites, and other application data.

We have already created our GitHub repository and set up the initial Spring Boot application. The application has also been successfully deployed on Render and is accessible through a public URL.

**GitHub Repository:** [https://github.com/drippinmuh/Group-10](https://github.com/drippinmuh/Group-10)

**Render Application:** [https://group-10-2hy5.onrender.com/](https://group-10-2hy5.onrender.com/)

## First Group Meeting and Team Responsibilities

In our group chat, we discussed replacing our initial Lost & Found idea with an SFU specific marketplace. The team agreed on this new direction because it offers useful, separable features solving a real-problem on campus.

- **Member 1: Maxim Sarukhanov** - Second-year Computing Science student. I’m currently working as a research assistant in a tangent lab, and also as a Full-stack developer for one company back in Kazakhstan. I selected safety and moderation epic because I’m interested in working on features like reporting suspicious listings, preventing scams, and helping users feel more comfortable on our marketplace.

- **Member 2: Ethan Cratchley** - Fourth-year Computer Science and Business Joint Major student specializing in Artificial Intelligence. I have done previous internships as a Software Engineer in the insurance and healthcare space with full-stack development experience. A majority of my work has been on the backend utilizing Python and Typescript building both internal tools and consumer facing web apps. I will utilize these skills and past experiences to take ownership of the User Accounts and Verification epic.

- **Member 3: Oleh Kutryk** - 3rd year Computing Science student. I have some experience with designing and developing backend applications. I will be working on *Epic 2: Creating and Managing Listings.* That includes: developing a listing model, implementing CRUD for listings, securing private endpoints.

- **Member 4: Jashith Garg** - 2nd-year Computing Science student at SFU. I have basic experience with Java, web development, and databases from coursework and personal projects. I will be taking ownership of *Epic 3: Search, Filters, Ratings, and Favourites* to focus on backend database querying, search filtering, and building user-friendly UI features like saved items and seller reviews.

- **Member 5: Faraz Seyfi** - Third-year Computer Engineering student at SFU. I have experience with Java, Python, C/C++, and web development through coursework and personal projects. I will be taking ownership of Epic 4: Buyer–Seller Communication, focusing on implementing messaging between buyers and sellers, managing conversations, and helping students coordinate item pickups on campus.
