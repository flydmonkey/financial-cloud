# open-registration Specification

## Purpose

Enables public self-registration so new users can enter the product without an administrator provisioning accounts, then create their own account books.

## Requirements

### Requirement: Public self-registration
The system MUST provide a public registration entry from the login experience. A visitor MUST be able to create an account with username, password, and display name without an existing invitation. This phase MUST NOT require email or phone verification.

#### Scenario: Successful registration
- **WHEN** a visitor submits a valid unused username, password meeting policy, and display name
- **THEN** the system creates an active user account and allows immediate login

#### Scenario: Duplicate username rejected
- **WHEN** a visitor registers with a username that already exists
- **THEN** the system rejects registration with a clear username-conflict error and creates no account

### Requirement: Registered user can create a book
A successfully registered user MUST be able to create a new account book. The creator MUST become the book administrator for that book (book-scoped administrator role and book access grant).

#### Scenario: First book after registration
- **WHEN** a newly registered user with no books creates a book
- **THEN** the book is created, the user receives access to it, and the user holds the administrator product role for that book

#### Scenario: Additional books
- **WHEN** a registered user who already owns or can access books creates another book
- **THEN** the new book is created and the creator is administrator only for the newly created book
