# Coin account app - Demo Project  

## Project Overview

The "Coin account" project is a demo application built using **Spring Boot** to manage and track financial ledgers.
It provides a **REST API** for handling basic account operations, including creating accounts,
checking balances, and processing transactions between accounts.


### Key Features
1. **Account Management**: Create and retrieve accounts, including balance queries.
2. **Transaction Processing**: Facilitate the movement of funds between accounts.
3. **REST API**: Exposes API endpoints to interact with the system programmatically using JSON requests.
4. **Spring Boot**: Demonstrate your understanding of dependency injection, REST controllers, and service layers.
5. **Gradle Build System**: Use Gradle to build and manage dependencies.
6. **Unit Testing**: Utilize **JUnit 5** to ensure code quality through unit tests.

---

## Use Case

This system represents a simplified financial ledger where multiple accounts are tracked. You can move funds between these ledgers through transactions.

The project can be extended to serve as the backbone of a wallet system, accounting software,
or any financial transaction platform.

For example:
- You create two accounts: a checking account and a savings account.
- The system allows you to transfer money between these accounts and retrieve the updated balance of each account after each transaction.

---

## Technical Stack

- **Java**: The core programming language for implementing business logic.
- **Spring Boot**: The framework for dependency injection, REST API creation, and other services.
- **Spring Data JPA**: Used to interact with the database for persisting ledger and account information.
- **Gradle**: The build tool for managing dependencies and automating tasks.
- **H2 Database**: An in-memory database used for data persistence in this demo application (can be swapped with a real database).
- **JUnit 5**: The testing framework to ensure your code is well-tested.

---

## API Endpoints

1. **Account Endpoints**
    - `GET /api/accounts/{id}`: Retrieve the account details by its ID, including balance.

2. **Transaction Endpoints**
    - `POST /api/transactions`: Create a new transaction between two accounts.

---

## Existing API Controller

The project includes a partially implemented controller, `CoinApi`. You are expected to extend the functionality of this controller as part of the assessment. 
Specifically, you will need to address the following methods:
- `getAccount`: Extend this method to return the current information and balance of an account.
- `newTransaction`: Implement this method to perform a transaction between two accounts.

---

## Candidate Tasks

Here are some potential tasks to assess your understanding and skills in Java, Spring Boot, and REST API development:

### 1. **Extend the `getAccount` Method to Show the Current Balance**

#### Task Details:
- Currently, the `getAccount` method only returns the account details.
- Modify this method to also return the current **balance** of the account.

#### Tips:
- The balance can be retrieved from the `Account` entity.
- Consider adding logic in the service layer (if applicable) or extending the repository method to fetch both the account details and balance.

#### Example Expected Response:
```json
{
  "id": 1,
  "name": "Savings Account",
  "balance": 1000.00
}
```

### 2. **Extend the `newTransaction` Method to allow transaction**

#### Task Details:
- You should enable users of the API to make transactions from one account to another.
- Ensure that the sending balance of an account remains below the `maximalOverdraft` limit.

### General Tips
- Feel free to change the provided API structure and/or request/response formats as you see fit to create a more streamlined API.
- Refactor the project structure if necessary (e.g., introduce a service layer, use DTOs, etc.).
- Include additional tests to demonstrate your implemented functions.
- Incorporate sanity checks for API calls as you deem appropriate.


### 3. **(Optionally) Create a docker image**

#### Task Details:
- Provide a way to build a runnable Docker image of the application (Dockerfile)
- Feel free to provide any suitable way to build a docker image (shell-script or gradle task or whatever suits best)


## Our expectations
We don’t expect a fully polished application. Please dedicate around 2-4 hours to the project
and take it as far as you get in this time.

As developers, we know it’s always possible to spend more time and a project never is really perfect. :-)  
But we’re just looking for an overview of your approach. Feel free to focus on areas you 
consider most important or where you’d like to showcase your skills. 

You can either push your changes to the repository or open a Merge Request - whichever you prefer.

The aim of this task is to get a sense of how you tackle problems independently and how familiar you are with our Java stack. In our 
next meeting at the office, we’ll review your work together and discuss your approach, including areas where you 
encountered challenges or where you’d take the project further.


## Getting Started

### Import

In Intellij:
 - Choose **File** -> **New** -> **Project from Version Control** and select "`git@gitlab.com:coinfinity/demoprojects/account-ledger-demoproject.git`"
 - Be sure to click **"Import from Gradle"** in the following PopUp
 - Enable Lombok (see below)
 - Try to run it -> The run-config **"LedgerApplication"** should start the project


### Lombok

Enable `Annotation Processing` in `File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors`
 
 
### H2 Database

Open `http://localhost:8090/h2-console` in your browser and use `jdbc:h2:./ledgerDb.h2` to connect, or use the integrated Database Explorer in IntelliJ (a database configuration is committed in the repo).

If you are using IntelliJ Community Edition, you will need to install the "Database Explorer" plugin and set up the database manually.
