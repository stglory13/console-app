Create a Java application without using any application frameworks.
The application reads commands from the console (STDIN) and processes them asynchronously into database.

Supported Commands
1. LOGIN(user_id)
* Logs in a user.
* A user who is already logged in cannot log in again.

2. LOGOUT(user_id)
* Logs out a user.
* A user who is not logged in cannot be logged out.

3. DATA_MODIFY(user_id)
* Indicates that a logged-in user is modifying data.
* If the user is not logged in, the command should be ignored.
* Multiple commands with the same user_id must result to multiple modification entries

4. STATS()
*  Prints application statistics:
* Number of currently logged-in users
* Number of data modifications per user

5. EXIT()
* Gracefully terminates the application.

Commands must be processed asynchronously and the application must be thread-safe.
Invalid commands must not stop or block the application You can choose any persistence solution.
Application functionality has to be supported by at leat one unit test.
Optionally simulate concurrent user work with another test.
Goal of this exercise is to show Java language and JDK know-how, OOP principles, clean code understanding,
concurrent programming knowledge, unit testing experience.

Solution  „event-driven console system“