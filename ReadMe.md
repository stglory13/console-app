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
Accepted commandId=cmd-login-user1-001
Completed commandId=cmd-login-user1-001 LOGIN: user logged in: user1

Accepted commandId=cmd-data-modify-user1-002
Completed commandId=cmd-data-modify-user1-002 DATA_MODIFY saved for user: user1

Accepted commandId=cmd-stats-003
Completed commandId=cmd-stats-003 STATS
Logged in users: 1
Data modifications per user: {user1=1}
```

---

## Notes

- Uses H2 in-memory database
- Thread-safe via queue and immutable objects
- No frameworks (pure Java + JDBC)
- Clean separation: producer / queue / worker / processor / repository
