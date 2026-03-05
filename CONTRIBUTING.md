# Contributing to OllamaFX 🚀

First of all, thank you for being here! We are building the fastest, most efficient native desktop interface for Ollama,
and your help is vital to making OllamaFX a greta project of Open Source Software.

By contributing to this project, you agree to help us maintain a high-quality, secure, and performant ecosystem.

## 🏗️ Technical Stack & Environment Setup

Before you start coding, ensure your development environment meets these requirements:

**JDK:** 17 or higher (21 recommended for long-term support).

**Build Tool:** Gradle.

**JavaFX:** Ensure you have the JavaFX SDK configured or use the provided build scripts.

**Ollama:** A local instance of Ollama must be running on your machine.

**IDE:** We recommend VScode, Antigravity, IntelliJ IDEA but any editor works.

## Setup Steps:

**Fork the repository.**

Clone your fork: git clone https://github.com/YOUR_USER/OllamaFX.git

Install dependencies: Gradle Build

Run the application: ./gradlew run

## 🔄 Development Workflow (The "GitFlow" Way)
To maintain stability, we follow a strict branching model. Never work directly on the main branch.

**Branching:** Always create a branch from develop.

**Features:** feat/amazing-feature

**Bug fixes:** fix/button-crash

**Documentation:** docs/api-guide

**Stay Updated:** Before starting, pull the latest changes: **git pull origin develop**

Pull Requests (PR):

Target the develop branch for your PR.

Describe your changes clearly.

Link the PR to an existing Issue (e.g., "Closes #42").

## 📜 Commit Message Standards
We use Conventional Commits. This allows us to automate our changelogs and versioning. Your commit messages must follow this format:

type(scope): description

Common types:

**feat:** A new feature for the user.

**fix:** A bug fix.

**docs:** Documentation only changes.

**style:** Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).

**refactor:** A code change that neither fixes a bug nor adds a feature.

**perf:** A code change that improves performance.

**chore:** Updating build tasks, package manager configs, etc.

Example: feat(ui): add dark mode toggle

## 🛠️ Coding Standards
To keep the codebase clean and maintainable:

Java Naming: Follow standard Java naming conventions (CamelCase for classes, camelCase for methods/variables).

Modularity: Keep the logic (Ollama API interaction) separated from the UI (JavaFX controllers).

Comments: Write "why" you did something, not "what" the code does. The code should be self-explanatory.

No Hardcoding: Use configuration files or environment variables for API endpoints or sensitive data.

## 🐛 Reporting Bugs & Suggestions
If you find a bug or have a great idea:

Check the Issues tab to see if it has already been reported.

Use our Issue Templates (Bug Report or Feature Request).

Provide as much detail as possible: OS version, Java version, and steps to reproduce.

## 🛡️ Security Policy
If you discover a security vulnerability, please do not open a public issue. Send an email to 
your-email@example.com (or use GitHub's private vulnerability reporting). 
We take security seriously to protect our users' local data.

## ⚖️ License
By contributing to OllamaFX, you agree that your contributions will be licensed under the project's Apache License 2.0 (or the license you chose).
