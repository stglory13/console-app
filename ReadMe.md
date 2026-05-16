# Assignment

Create a Java application without using any application frameworks.  
The application reads commands from the console (STDIN) and processes them asynchronously into database.

---

## Supported Commands

### 1. LOGIN(user_id)
- Logs in a user.
- A user who is already logged in cannot log in again.

### 2. LOGOUT(user_id)
- Logs out a user.
- A user who is not logged in cannot be logged out.

### 3. DATA_MODIFY(user_id)
- Indicates that a logged-in user is modifying data.
- If the user is not logged in, the command should be ignored.
- Multiple commands with the same user_id must result to multiple modification entries

### 4. STATS()
- Prints application statistics:
    - Number of currently logged-in users
    - Number of data modifications per user

### 5. EXIT()
- Gracefully terminates the application.

---

## Requirements

- Commands must be processed asynchronously and the application must be thread-safe.
- Invalid commands must not stop or block the application.
- You can choose any persistence solution.
- Application functionality has to be supported by at least one unit test.
- Optionally simulate concurrent user work with another test.

---

## Goal

Goal of this exercise is to show Java language and JDK know-how, OOP principles, clean code understanding, concurrent programming knowledge, unit testing experience.

---

# Solution

## Implemented Application

This solution is implemented as an event-driven console application using pure Java (no frameworks).

The application processes user commands asynchronously using a producer-consumer architecture and persists data into an H2 database.

## Application Structure

The application follows a clean separation of responsibilities across core components:

### Description

- **Main**

  Acts as the composition root. It wires all components together and starts the application.

- **UserSessionState**

  Holds runtime state in memory. Tracks currently logged-in users in a thread-safe way.

- **Repository**

  Responsible for persistence. Stores modification events into the database (H2 via JDBC).

- **CommandProcessor**

  Contains business logic. Interprets commands and applies rules (login, logout, stats, etc.).

---

Key characteristics:
- Immutable command events
- Asynchronous processing via queue and worker thread
- Thread-safe design
- Clear separation of concerns
- JDBC-based persistence

---

## User Manual

### How to run

```bash
./gradlew run
```

### Example commands

```
LOGIN(user1)
DATA_MODIFY(user1)
DATA_MODIFY(user1)
STATS()
LOGOUT(user1)
EXIT()
```

### Supported commands

- LOGIN(user_id) – logs in a user
- LOGOUT(user_id) – logs out a user
- DATA_MODIFY(user_id) – records a modification for a logged-in user
- STATS() – prints number of logged-in users and number of modifications per user
- EXIT() – gracefully terminates the application

---

## Architecture

### Event-driven processing pipeline

```
Console (STDIN)
      ↓
ConsoleCommandProducer
      ↓
CommandQueue
      ↓
CommandWorker (thread)
      ↓
CommandProcessor
      ↓
 ┌─────────────────────────────┐
 │ UserSessionState (in-memory)│
 │ JdbcModificationRepository  │
 └─────────────────────────────┘
```

### Design overview

- Commands are modeled as immutable events
- Producer reads input and generates command events
- Queue decouples input from processing
- Worker processes commands asynchronously
- Processor applies business logic
- Repository persists modification events into database

---

## Shutdown flow (EXIT)

```
CommandProcessor → logs EXIT processed
CommandWorker    → stops processing loop
Main             → waits for worker thread (join)
Repository       → closed via try-with-resources
```

This ensures graceful termination of the application.

---

## Example output

```
[INPUT] LOGOUT(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user1-001 | cmd=LOGOUT
[INPUT] LOGIN(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user1-002 | cmd=LOGIN
[INPUT] LOGIN(user2)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user2-003 | cmd=LOGIN
[INPUT] LOGIN(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user1-004 | cmd=LOGIN
[INPUT] LOGIN()
[console-app][21:24:20] > ACCEPTED  | id=cmd-invalid-005 | cmd=INVALID
[INPUT] LOGIN
[console-app][21:24:20] > ACCEPTED  | id=cmd-invalid-006 | cmd=INVALID
[INPUT] DATA_MODIFY()
[console-app][21:24:20] > ACCEPTED  | id=cmd-invalid-007 | cmd=INVALID
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user1-001 | cmd=LOGOUT | user not logged in: user1
[INPUT] HELLO(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-invalid-008 | cmd=INVALID
[console-app][21:24:20] > COMPLETED | id=cmd-login-user1-002 | cmd=LOGIN | user logged in: user1
[INPUT] INVALID_COMMAND()
[console-app][21:24:20] > COMPLETED | id=cmd-login-user2-003 | cmd=LOGIN | user logged in: user2
[console-app][21:24:20] > ACCEPTED  | id=cmd-invalid-009 | cmd=INVALID
[console-app][21:24:20] > COMPLETED | id=cmd-login-user1-004 | cmd=LOGIN | user already logged in: user1
[INPUT] DATA_MODIFY(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user1-010 | cmd=DATA_MODIFY
[console-app][21:24:20] > COMPLETED | id=cmd-invalid-005 | cmd=INVALID | command ignored: LOGIN()
[console-app][21:24:20] > COMPLETED | id=cmd-invalid-006 | cmd=INVALID | command ignored: LOGIN
[console-app][21:24:20] > COMPLETED | id=cmd-invalid-007 | cmd=INVALID | command ignored: DATA_MODIFY()
[INPUT] DATA_MODIFY(user1)
[console-app][21:24:20] > COMPLETED | id=cmd-invalid-008 | cmd=INVALID | command ignored: HELLO(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user1-011 | cmd=DATA_MODIFY
[console-app][21:24:20] > COMPLETED | id=cmd-invalid-009 | cmd=INVALID | command ignored: INVALID_COMMAND()
[INPUT] DATA_MODIFY(user2)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user2-012 | cmd=DATA_MODIFY
[INPUT] DATA_MODIFY(user3)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user3-013 | cmd=DATA_MODIFY
[INPUT] LOGOUT(user2)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user2-014 | cmd=LOGOUT
[INPUT] LOGOUT(user2)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user2-015 | cmd=LOGOUT
[INPUT] DATA_MODIFY(user2)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user2-016 | cmd=DATA_MODIFY
[INPUT] LOGIN(user3)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user3-017 | cmd=LOGIN
[INPUT] DATA_MODIFY(user3)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user3-018 | cmd=DATA_MODIFY
[INPUT] LOGIN(user4)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user4-019 | cmd=LOGIN
[INPUT] DATA_MODIFY(user4)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user4-020 | cmd=DATA_MODIFY
[INPUT] DATA_MODIFY(user4)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user4-021 | cmd=DATA_MODIFY
[INPUT] LOGOUT(user4)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user4-022 | cmd=LOGOUT
[INPUT] DATA_MODIFY(user4)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user4-023 | cmd=DATA_MODIFY
[INPUT] LOGIN(user5)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user5-024 | cmd=LOGIN
[INPUT] LOGOUT(user5)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user5-025 | cmd=LOGOUT
[INPUT] DATA_MODIFY(user5)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user5-026 | cmd=DATA_MODIFY
[INPUT] LOGIN(user99)
[console-app][21:24:20] > ACCEPTED  | id=cmd-login-user99-027 | cmd=LOGIN
[INPUT] DATA_MODIFY(user99)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user99-028 | cmd=DATA_MODIFY
[INPUT] DATA_MODIFY(user99)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user99-029 | cmd=DATA_MODIFY
[INPUT] DATA_MODIFY(user99)
[console-app][21:24:20] > ACCEPTED  | id=cmd-data-modify-user99-030 | cmd=DATA_MODIFY
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user1-010 | cmd=DATA_MODIFY | saved for user: user1
[INPUT] STATS()
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user1-011 | cmd=DATA_MODIFY | saved for user: user1
[console-app][21:24:20] > ACCEPTED  | id=cmd-stats-031 | cmd=STATS
[INPUT] LOGOUT(user1)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user1-032 | cmd=LOGOUT
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user2-012 | cmd=DATA_MODIFY | saved for user: user2
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user3-013 | cmd=DATA_MODIFY | ignored, user not logged in: user3
[INPUT] LOGOUT(user1)
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user2-014 | cmd=LOGOUT | user logged out: user2
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user1-033 | cmd=LOGOUT
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user2-015 | cmd=LOGOUT | user not logged in: user2
[INPUT] LOGOUT(user3)
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user2-016 | cmd=DATA_MODIFY | ignored, user not logged in: user2
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user3-034 | cmd=LOGOUT
[console-app][21:24:20] > COMPLETED | id=cmd-login-user3-017 | cmd=LOGIN | user logged in: user3
[INPUT] LOGOUT(user99)
[console-app][21:24:20] > ACCEPTED  | id=cmd-logout-user99-035 | cmd=LOGOUT
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user3-018 | cmd=DATA_MODIFY | saved for user: user3
[console-app][21:24:20] > COMPLETED | id=cmd-login-user4-019 | cmd=LOGIN | user logged in: user4
[INPUT] EXIT()
[console-app][21:24:20] > ACCEPTED  | id=cmd-exit-036 | cmd=EXIT
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user4-020 | cmd=DATA_MODIFY | saved for user: user4
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user4-021 | cmd=DATA_MODIFY | saved for user: user4
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user4-022 | cmd=LOGOUT | user logged out: user4
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user4-023 | cmd=DATA_MODIFY | ignored, user not logged in: user4
[console-app][21:24:20] > COMPLETED | id=cmd-login-user5-024 | cmd=LOGIN | user logged in: user5
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user5-025 | cmd=LOGOUT | user logged out: user5
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user5-026 | cmd=DATA_MODIFY | ignored, user not logged in: user5
[console-app][21:24:20] > COMPLETED | id=cmd-login-user99-027 | cmd=LOGIN | user logged in: user99
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user99-028 | cmd=DATA_MODIFY | saved for user: user99
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user99-029 | cmd=DATA_MODIFY | saved for user: user99
[console-app][21:24:20] > COMPLETED | id=cmd-data-modify-user99-030 | cmd=DATA_MODIFY | saved for user: user99
[console-app][21:24:20] > COMPLETED | id=cmd-stats-031 | cmd=STATS | statistics printed
[console-app][21:24:20] > Logged in users: 3
[console-app][21:24:20] > Data modifications per user: {user1=2, user99=3, user2=1, user3=1, user4=2}
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user1-032 | cmd=LOGOUT | user logged out: user1
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user1-033 | cmd=LOGOUT | user not logged in: user1
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user3-034 | cmd=LOGOUT | user logged out: user3
[console-app][21:24:20] > COMPLETED | id=cmd-logout-user99-035 | cmd=LOGOUT | user logged out: user99
[console-app][21:24:20] > COMPLETED | id=cmd-exit-036 | cmd=EXIT | shutdown requested

Process finished with exit code 0

```

---

## Notes

- Uses H2 in-memory database
- Thread-safe via queue and immutable objects
- No frameworks (pure Java + JDBC)
- Clean separation: producer / queue / worker / processor / repository
