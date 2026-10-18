# Contributing Guidelines

These guidelines explain how our team will collaborate on the SFU Marketplace project for CMPT 276.

## 1. Branching

- The `main` branch should always contain stable, working code.
- Each team member should create a separate branch for their task.
- Use descriptive branch names, such as:
  - `feature/authentication`
  - `feature/listings`
  - `feature/search`
  - `feature/messaging`
  - `fix/login-error`
- Do not push directly to `main`.

## 2. Workflow

Before starting a new task, update your local repository:

```bash
git switch main
git pull origin main
git switch -c feature/your-feature
```

After making changes, commit and push them:

```bash
git add .
git commit -m "Describe your changes"
git push -u origin feature/your-feature
```

Then open a Pull Request on GitHub to merge your changes into `main`.

## 3. Pull Requests and Code Review

- All changes must go through Pull Requests.
- At least one other team member should review the PR before merging.
- Explain what you changed in the PR description.
- Fix any issues found during review.
- Keep Pull Requests focused on one feature or task.

## 4. Testing

Before submitting a Pull Request:

- Make sure the application builds successfully.
- Run relevant tests.
- Check that your changes do not break existing features.

For the Spring Boot application, run:

```bash
./mvnw test
```

## 5. Communication

- Use our Discord channel to discuss tasks, questions, and problems.
- Let the team know before making major changes to shared files.
- Ask for help if you run into issues.
- Coordinate with other members when working on related features.

## 6. General Rules

- Write clear and understandable code.
- Use meaningful commit messages.
- Do not commit passwords, API keys, or other sensitive information.
- Respect other team members' work.
- Keep the GitHub repository organized.

## 7. Deployment

Our application is deployed on Render:

https://group-10-2hy5.onrender.com/

The `main` branch is used for deployment. Changes merged into `main` will be deployed automatically if Auto-Deploy is enabled.

---

These guidelines can be updated if the team agrees on changes.
