# Group 10: SFU Marketplace

## Project Abstract

SFU Marketplace is a web application where students can buy and sell second-hand items like textbooks, electronics, furniture and more. Students will be able to create listings, search for items, mark favourites and contact sellers. Our goal is to make buying and selling more convenient for SFU students.

## Problem and Target Market

SFU students often have items they no longer need, such as textbooks, while other students may be looking for exactly those items but don't want to pay full price for them. Sometimes students are even willing to give items away for free. However, it can be difficult to find other students to buy from or sell to, since there is no convenient marketplace for the SFU community.

Our target audience is SFU students, especially those looking to save money by buying second-hand items or selling things they no longer need. Our project will make it easier to find affordable items.

## Existing Solutions

Students currently buy and sell used items through general platforms such as Facebook Marketplace and Craigslist, or through SFU group chats and Discord servers. General marketplaces are open to everyone, so SFU listings are mixed in with sellers from across the Lower Mainland, and there is no way to confirm that the other person is a student. Group chats are local to SFU, but posts are quickly buried by new messages and cannot be searched or filtered.

Two SFU-specific options exist. u-Shop SFU is a marketplace for SFU students, but it is only available as a mobile app on Google Play, so students using an iPhone or a laptop cannot access it. SFU-Market is a project on GitHub that started at a hackathon. It is still marked as in development, and when we tested it, its builds failed and the site returned a server error. As a result, there is currently no reliable, browser-based marketplace built for the SFU community.

## Our Solution

Our approach to this problem is to create a simple and user-friendly website where SFU students can buy and sell used items. Unlike SFU's u-Shop, which is a mobile app, our marketplace will run directly in a web browser, so students won't need to download anything. Another similar project, SFU-Market, was created during a hackathon and is still listed on GitHub as "in development". Our goal is to build a fully-fledged and reliable website that students can easily access and use. We also plan to include useful features such as search filters, favourites, messaging, and meeting scheduling. We want to make buying and selling within the SFU community as simple and convenient as possible.

## Scope and Major Epics

SFU Marketplace is made up of several individual features built around a central shared listing system, rather than one main feature with many subproblems. We divided the project into five major epics, one per person, with each epic including several smaller tasks to be designed as tickets, implemented and tested.

### Epic 1: User Accounts and Verification

Users will be able to register, log in, log out and manage a profile with their name, preferred campus and active listings. Registration will be restricted to SFU email addresses, and new accounts will be confirmed through an email verification link. Passwords will be stored securely and can be reset if a user forgets their password. This epic also defines the user roles that the moderation tools in Epic 5 will use.

### Epic 2: Creating and Managing Listings

Users will be able to view listings as a list and view a selected listing. Each listing will include a title, description, picture, price, and some information about the seller. Users who are logged in and verified can create a new listing. A listing can be edited or deleted by its owner.

### Epic 3: Search, Filters, Ratings, and Favourites

Users will be able to search and filter items by keyword, price, condition, category, campus location, and course code (e.g., CMPT 276). Students can also save items to a personal Favourites list and view or leave seller ratings to build community trust.

### Epic 4: Messaging and Meetup Coordination

Users will be able to send and receive messages to communicate with buyers and sellers. They can ask questions about items, discuss prices, and arrange a time and place to meet. We will use the OpenStreetMap Nominatim API to help users find convenient meetup locations around SFU.

### Epic 5: Safety and Moderation

To make the marketplace more reliable, users will be able to report suspicious or inappropriate listings. We plan to include basic moderation tools that allow administrators to review reports and remove listings that violate the platform's rules.

## External Web APIs

We plan to use the [OpenStreetMap Nominatim REST API](https://nominatim.org/release-docs/latest/api/Overview/) to help students find meetup locations around SFU. The API allows us to search for places and receive their names and coordinates in JSON format. Our Spring Boot application can use this information to help buyers and sellers arrange pickups. We will follow the API's usage limits and cache results to avoid unnecessary requests.

## Technology and Initial Setup

For the frontend, we plan to use HTML, CSS, JavaScript, Thymeleaf, and Bootstrap. Thymeleaf will allow us to create dynamic web pages using Spring Boot, while Bootstrap will help us build a user-friendly interface. For the database, we will use PostgreSQL to store user accounts, product listings, messages, favourites, and other application data.

We have already created our GitHub repository and set up the initial Spring Boot application. The application has also been successfully deployed on Render and is accessible through a public URL.

- **GitHub Repository:** https://github.com/drippinmuh/Group-10
- **Render Application:** https://group-10-2hy5.onrender.com/

## First Group Meeting and Team Responsibilities

Our first meeting was held on October 18th over Discord. We introduced ourselves, discussed our backgrounds, and replaced our initial lost-and-found idea with an SFU-specific marketplace, because it solves a more common problem on campus and splits naturally into five features. Before our next meeting, each member will break their epic into tasks, we will test the Nominatim API, and we will design the database tables for users, listings and messages. We plan to meet once or twice a week for about 15 minutes.

**Member 1: Maxim Sarukhanov.** Second-year Computing Science student. I'm currently working as a research assistant in a tangent lab, and also as a full-stack developer for a company back in Kazakhstan. I selected the Safety and Moderation epic because I'm interested in working on features like reporting suspicious listings, preventing scams, and helping users feel more comfortable on our marketplace.

**Member 2: Ethan Cratchley.** Fourth-year Computer Science and Business Joint Major student specializing in Artificial Intelligence. I have done previous internships as a Software Engineer in the insurance and healthcare space with full-stack development experience. A majority of my work has been on the backend, using Python and TypeScript to build both internal tools and consumer-facing web apps. I will use these skills and past experiences to take ownership of the User Accounts and Verification epic.

**Member 3: Oleh Kutryk.** Third-year Computing Science student. I have some experience with designing and developing backend applications. I will be working on Epic 2: Creating and Managing Listings, which includes developing a listing model, implementing CRUD for listings, and securing private endpoints.

**Member 4: Jashith Garg.** Second-year Computing Science student at SFU. I have basic experience with Java, web development, and databases from coursework and personal projects. I will be taking ownership of Epic 3: Search, Filters, Ratings, and Favourites, focusing on backend database querying, search filtering, and building user-friendly UI features like saved items and seller reviews.

**Member 5: Faraz Seyfi.** Third-year Computer Engineering student at SFU. I have experience with Java, Python, C/C++, and web development through coursework and personal projects. I will be taking ownership of Epic 4: Messaging and Meetup Coordination, focusing on implementing messaging between buyers and sellers, managing conversations, and helping students coordinate item pickups on campus.
