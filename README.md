# Application Health & Uptime Monitoring System

A full-stack web application for monitoring the availability and
response time of websites and HTTP APIs. The system periodically checks
configured endpoints, records each check in PostgreSQL, and provides a
frontend for viewing monitor status and check history.

> **Project status:** The backend has been tested locally. The automated
> test suite most recently completed with **19 tests passed, 0 failures,
> and 0 errors**. Deployment is a separate step and is not yet
> represented as complete in this README.

## Table of Contents

-   [Overview](#overview)
-   [Problem Statement](#problem-statement)
-   [Objectives](#objectives)
-   [Key Features](#key-features)
-   [How the System Addresses Real-World
    Problems](#how-the-system-addresses-real-world-problems)
-   [Architecture](#architecture)
-   [Monitoring Workflow](#monitoring-workflow)
-   [Technology Stack](#technology-stack)
-   [Project Structure](#project-structure)
-   [Database Design](#database-design)
-   [API Overview](#api-overview)
-   [Prerequisites](#prerequisites)
-   [Local Setup](#local-setup)
-   [Configuration](#configuration)
-   [Run the Application](#run-the-application)
-   [Run Tests](#run-tests)
-   [Example API Requests](#example-api-requests)
-   [Reliability and Security
    Considerations](#reliability-and-security-considerations)
-   [Known Limitations and Future
    Enhancements](#known-limitations-and-future-enhancements)
-   [Deployment Overview](#deployment-overview)
-   [Contributing](#contributing)
-   [Author](#author)

## Overview

Websites and APIs are expected to be available and responsive. If a
service becomes unreachable, starts returning unexpected HTTP status
codes, or responds slowly, users and dependent applications can be
affected before the team notices the problem.

The **Application Health & Uptime Monitoring System** automates
recurring HTTP/HTTPS checks and stores the results as a history that can
be reviewed later. A Spring Boot backend performs monitoring and exposes
REST APIs, PostgreSQL stores monitor configuration and check results,
and a React frontend provides the user interface.

## Problem Statement

Manual monitoring is difficult to maintain consistently and does not
provide a dependable historical record. A simple one-time check also
cannot show when a service failed, how long it was slow, or whether a
problem is recurring.

This project addresses those needs by running scheduled checks,
measuring response time, recording HTTP status and success/failure
outcomes, and retaining check history in a database.

## Objectives

-   Check configured websites and API endpoints automatically.
-   Measure and record request response time.
-   Record HTTP status codes and whether each check succeeded.
-   Store check history for later investigation and troubleshooting.
-   Expose backend functionality through REST endpoints.
-   Keep monitoring logic in the backend rather than relying on a
    browser tab to remain open.
-   Use automated tests to validate core service behavior, URL
    validation, and concurrent monitoring behavior.

## Key Features

-   **Scheduled monitoring:** monitors can be checked repeatedly
    according to their configured interval.
-   **Manual checks:** request a check for a monitor through the REST
    API.
-   **Response-time tracking:** records how long a check took.
-   **HTTP status tracking:** stores the returned HTTP status code when
    available.
-   **Check history:** retrieves previous results for a monitor.
-   **Expected-status configuration:** monitor configuration includes an
    expected HTTP status.
-   **Timeout configuration:** monitor configuration includes a request
    timeout.
-   **URL validation:** validates monitor URLs before they are used.
-   **Concurrency testing:** automated tests cover concurrent
    monitoring-service behavior.
-   **PostgreSQL persistence:** monitor data and check results are
    stored in a relational database.
-   **Health endpoint:** Spring Boot Actuator exposes application health
    information.

Feature availability depends on the current implementation. Notification
delivery, public status pages, authentication, and analytics should not
be considered implemented unless they are added and verified in the
codebase.

## How the System Addresses Real-World Problems

  -----------------------------------------------------------------------
  Real-world problem      Project approach        Benefit
  ----------------------- ----------------------- -----------------------
  A website goes down     Scheduled endpoint      Reduces reliance on
  without being noticed   checks                  manual checks

  A service responds      Record response time    Helps identify latency
  slowly                  for each check          changes and slow
                                                  periods

  An API returns an       Compare the result with Makes unsuccessful
  unexpected status code  the monitor's expected  checks easier to
                          status                  identify

  A failure is            Persist check history   Helps investigate
  intermittent                                    repeated or short-lived
                                                  failures

  Someone needs to        Keep historical check   Provides evidence
  investigate an incident records                 beyond the service's
  after it recovers                               current state

  Invalid URLs are        URL validation before   Avoids unnecessary
  entered                 checking                requests to malformed
                                                  targets

  Multiple checks run     Concurrency-related     Helps verify that
  close together          service tests and       concurrent operations
                          controlled monitoring   behave correctly
                          logic                   

  The UI is closed        Run monitoring in the   Checks do not depend on
                          backend scheduler       a user keeping the
                                                  browser open
  -----------------------------------------------------------------------

### Example scenario

Suppose an API normally responds with HTTP `200`, but later begins
returning HTTP `503`.

1.  The scheduled monitoring service runs a check.
2.  The result records the response status, response time, check
    timestamp, and success outcome.
3.  The result is saved to PostgreSQL.
4.  The dashboard or check-history API can display the recorded result.
5.  A developer can compare recent checks with earlier results to
    investigate the incident.

This workflow records evidence of a problem. **It does not, by itself,
guarantee an alert is sent to a person.** Email, SMS, webhook
notifications, and configurable consecutive-failure alert thresholds are
future enhancements unless separately implemented.

## Architecture

``` mermaid
flowchart TD
    U[User] --> FE[React Frontend]
    FE -->|HTTP REST requests| API[Spring Boot REST Controllers]
    API --> MS[Monitor Service]
    API --> MON[Monitoring Service]
    SCH[Scheduled Monitoring Scheduler] --> MON
    MON -->|HTTP or HTTPS request| TARGET[Monitored Website or API]
    MON -->|Save check result| DB[(PostgreSQL)]
    MS -->|Read or update monitor configuration| DB
    API -->|Return monitor data and check history| FE
    ACT[Spring Boot Actuator] --> HEALTH[Health Endpoint]
```

### Architecture layers

1.  **Frontend --- React and Vite**
    -   Provides the browser-based interface.
    -   Sends HTTP requests to the backend REST API.
    -   Presents monitor information and check results returned by the
        backend.
2.  **REST API --- Spring Boot controllers**
    -   Accepts frontend requests.
    -   Validates request input and delegates work to application
        services.
    -   Returns data as JSON.
3.  **Application services**
    -   `MonitorService` manages monitor-related operations.
    -   `MonitoringService` performs checks and processes check results.
    -   `MonitorUrlValidator` validates monitor URLs.
4.  **Scheduled monitoring**
    -   `MonitoringScheduler` triggers recurring checks for configured
        monitors.
    -   The configured interval determines how frequently a monitor is
        checked.
5.  **Persistence --- Spring Data JPA and PostgreSQL**
    -   Stores monitor configuration and monitoring history.
    -   Allows the application to retrieve previous check results.
6.  **Monitored services**
    -   External websites or APIs that the monitor requests over HTTP or
        HTTPS.

## Monitoring Workflow

``` mermaid
sequenceDiagram
    participant Scheduler as Monitoring Scheduler
    participant Service as Monitoring Service
    participant Target as Target Website/API
    participant DB as PostgreSQL
    participant UI as React Frontend

    Scheduler->>Service: Trigger due monitor check
    Service->>Target: Send HTTP/HTTPS request with timeout
    Target-->>Service: HTTP response or request failure
    Service->>Service: Measure response time and evaluate result
    Service->>DB: Persist check result
    UI->>Service: Request monitor check history through REST API
    Service->>DB: Read stored history
    DB-->>Service: Historical results
    Service-->>UI: Return JSON response
```

The precise behavior for connection failures, unexpected status codes,
and timeout handling is determined by the backend implementation.

## Technology Stack

  Layer                Technology
  -------------------- -------------------------------
  Frontend             React, Vite, JavaScript
  Backend              Java, Spring Boot
  API style            REST, JSON
  Persistence          PostgreSQL
  Database access      Spring Data JPA / Hibernate
  Connection pooling   HikariCP
  Health endpoint      Spring Boot Actuator
  Build tool           Apache Maven
  Testing              JUnit and Mockito-based tests
  Version control      Git and GitHub

The Maven project compiles source code for Java 21. A newer installed
JDK can run the project, but the build's configured Java release remains
21.

## Project Structure

The following is a guide to the main application areas. Exact frontend
component and configuration filenames may differ as the project evolves.

``` text
Up_time_monitory/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/vinoth/uptimemonitor/
│   │   │       ├── UptimeMonitorApplication.java
│   │   │       ├── controller/
│   │   │       │   └── MonitorController.java
│   │   │       ├── scheduler/
│   │   │       │   └── MonitoringScheduler.java
│   │   │       ├── service/
│   │   │       │   ├── MonitorService.java
│   │   │       │   ├── MonitoringService.java
│   │   │       │   └── MonitorUrlValidator.java
│   │   │       ├── repository/
│   │   │       └── model/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/vinoth/uptimemonitor/
│           ├── UptimeMonitorApplicationTests.java
│           └── service/
│               ├── MonitorServiceTest.java
│               ├── MonitorUrlValidatorTest.java
│               └── MonitoringServiceConcurrencyTest.java
└── frontend/
    ├── package.json
    ├── package-lock.json
    ├── index.html
    ├── public/
    └── src/
        ├── components/
        ├── pages/
        ├── services/
        └── ...
```

> **Note:** The tree shows the main architectural areas and known
> classes. Confirm the actual folders and filenames in the repository
> before treating every illustrative folder as present.

### Main backend components

-   `UptimeMonitorApplication.java` --- Spring Boot application entry
    point.
-   `MonitorController.java` --- exposes monitor and check-related REST
    operations.
-   `MonitorService.java` --- handles monitor configuration operations.
-   `MonitoringService.java` --- performs a monitoring check and
    processes the result.
-   `MonitoringScheduler.java` --- triggers recurring checks.
-   `MonitorUrlValidator.java` --- validates monitor URLs.
-   Repository classes --- access PostgreSQL through Spring Data JPA.
-   Model/entity classes --- represent persisted monitor and check data.

## Database Design

The application uses PostgreSQL to persist monitor configuration and
check history.

### Monitor

A monitor represents a website or API to check. The current
implementation uses monitor settings such as:

-   Monitor name
-   Target URL
-   Check interval in seconds
-   Request timeout in seconds
-   Expected HTTP status

The exact database column names and additional fields are defined by the
entity classes.

### Monitor Check

A monitor check represents one attempt to contact a target. The observed
API response includes fields such as:

-   Check ID
-   Monitor/check timestamp
-   Response time in milliseconds
-   HTTP status code, when available
-   Success/failure outcome

A historical list of checks makes it possible to review past behavior
instead of seeing only the current state.

## API Overview

The following endpoints have been exercised during local development:

  -----------------------------------------------------------------------------
  Method                  Endpoint                      Purpose
  ----------------------- ----------------------------- -----------------------
  `POST`                  `/api/monitors/{id}/check`    Trigger a manual check
                                                        for a monitor

  `GET`                   `/api/monitors/{id}/checks`   Retrieve recorded
                                                        checks for a monitor

  `GET`                   `/actuator/health`            Check application
                                                        health
  -----------------------------------------------------------------------------

Other monitor CRUD routes may be available in the controller. Refer to
the current `MonitorController.java` for the complete route list and
request formats.

### Example response for a check

``` json
{
  "id": 130,
  "checkedAt": "2026-09-25T12:52:27.185915",
  "responseTimeMs": 888,
  "statusCode": 200,
  "success": true
}
```

This is an illustrative example of the response shape observed during
development, not a guaranteed response for every request.

## Prerequisites

Install the following software:

-   **JDK 21 or newer** (the Maven build targets Java 21)
-   **Apache Maven** or the included Maven Wrapper
-   **PostgreSQL**
-   **Node.js and npm** for the React frontend
-   **Git**
-   A code editor such as VS Code

## Local Setup

### 1. Clone the repository

``` bash
git clone https://github.com/vino1408/Up_time_monitory.git
cd Up_time_monitory
```

### 2. Create the PostgreSQL database

Create a PostgreSQL database named `uptime_monitor`, or update the
database name in your local configuration to match the database you
create.

Example using `psql`:

``` sql
CREATE DATABASE uptime_monitor;
```

Use the PostgreSQL username and password configured on your own machine.

### 3. Configure database credentials

Configure the database URL, username, and password for your local
environment. Do not commit real passwords, tokens, or production
connection strings to GitHub.

For local development, use environment variables or an ignored local
configuration file. For example, a property can use Spring's
environment-variable syntax:

``` properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/uptime_monitor}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
```

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to values appropriate for
your environment. If the project currently uses a different property
setup, adapt it consistently rather than replacing working settings
blindly.

### 4. Start the backend

From the repository root:

``` bash
mvn spring-boot:run
```

On Windows, if the Maven Wrapper is working, you can instead run:

``` cmd
mvnw.cmd spring-boot:run
```

The backend is configured to use port `8080` by default.

### 5. Start the frontend

Open a second terminal:

``` bash
cd frontend
npm install
npm run dev
```

Vite prints the local frontend URL in the terminal, commonly
`http://localhost:5173`.

If the frontend cannot reach the backend, check the frontend API base
URL and the backend CORS configuration. Do not assume the development
URL will be correct after deployment.

## Configuration

Typical Spring Boot settings include:

  ---------------------------------------------------------------------------------
  Setting                                       Purpose
  --------------------------------------------- -----------------------------------
  `spring.datasource.url`                       PostgreSQL connection URL

  `spring.datasource.username`                  Database username

  `spring.datasource.password`                  Database password

  `server.port`                                 Backend HTTP port; defaults to
                                                `8080` in this project

  `spring.jpa.hibernate.ddl-auto`               Hibernate schema-management
                                                behavior

  `management.endpoints.web.exposure.include`   Actuator endpoints exposed over
                                                HTTP
  ---------------------------------------------------------------------------------

For production, use environment variables for secrets and a deliberate
database migration strategy. Avoid exposing detailed health information
or administrative endpoints publicly.

## Run the Application

1.  Start PostgreSQL and confirm the database is accessible.
2.  Start the Spring Boot backend.
3.  Confirm that the backend starts without database connection errors.
4.  Start the React frontend.
5.  Add or select a monitor in the UI, if the relevant screen is
    available.
6.  Trigger a manual check or wait for the scheduled interval.
7.  Inspect the check result and check history.
8.  Review backend logs if a check fails.

## Run Tests

Run the backend test suite from the project root:

``` bash
mvn -B clean test
```

On Windows, you can also use the Maven Wrapper if it is configured
correctly:

``` cmd
mvnw.cmd -B clean test
```

The most recently verified local run completed with **19 tests, 0
failures, and 0 errors**. Tests included monitor service behavior, URL
validation, concurrency behavior, and Spring Boot application startup.

Build the frontend separately:

``` bash
cd frontend
npm run build
```

## Example API Requests

### Trigger a manual check

``` bash
curl -X POST http://localhost:8080/api/monitors/1/check
```

Replace `1` with an existing monitor ID.

### Retrieve check history

``` bash
curl http://localhost:8080/api/monitors/1/checks
```

### Check backend health

``` bash
curl http://localhost:8080/actuator/health
```

The monitor must exist in the configured database for monitor-specific
requests to return meaningful results.

## Reliability and Security Considerations

-   **Timeouts:** configure a sensible request timeout so an
    unresponsive target does not hold a check indefinitely.
-   **URL validation:** validate targets before sending requests.
-   **Concurrency:** bound concurrent checks so a large monitor list
    does not overwhelm the application or the monitored services.
-   **Failure history:** persist each check result to support
    troubleshooting and historical review.
-   **Credentials:** store database passwords and other secrets in
    environment variables or a secrets manager.
-   **Production database:** use a managed PostgreSQL service or a
    properly maintained database server, backups, and access controls.
-   **Outbound request safety:** in a public deployment, restrict or
    block requests to loopback, private, link-local, and cloud metadata
    addresses as appropriate. URL validation should be designed to
    reduce server-side request forgery (SSRF) risk, including
    DNS-rebinding scenarios.
-   **CORS:** allow only the frontend origins that need access to the
    backend.
-   **Health checks:** expose only the health information needed by the
    hosting platform.
-   **Alerting:** a stored failure is not the same as a delivered
    notification. Alert channels need their own retry, deduplication,
    and delivery-failure handling.
-   **Monitoring the monitor:** production operation should include
    hosting-platform health checks and external monitoring of this
    service itself.

## Known Limitations and Future Enhancements

The following are possible next steps, not claims that they are already
implemented:

-   Email, SMS, Slack, or webhook alerts
-   Configurable consecutive-failure thresholds to reduce alerts caused
    by transient network errors
-   Uptime percentage calculations over selectable time ranges
-   Response-time charts and latency percentiles
-   Public status pages and incident timelines
-   User authentication, authorization, and multi-user support
-   Monitor pause/resume and maintenance windows
-   Retry policies and alert deduplication
-   Pagination and retention policies for large check histories
-   Database migrations with Flyway or Liquibase
-   Docker packaging and automated CI/CD
-   External monitoring of the monitoring service
-   Integration tests against a disposable PostgreSQL instance
-   Stronger SSRF defenses and restrictions on private network targets

## Deployment Overview

The planned hosting approach is:

-   **PostgreSQL:** Neon or another managed PostgreSQL provider
-   **Backend:** Render or another platform that supports Java/Spring
    Boot
-   **Frontend:** Vercel or another static frontend host compatible with
    Vite

Before deploying:

1.  Provision the hosted PostgreSQL database.
2.  Configure the backend's database URL, username, and password as
    hosting environment variables.
3.  Configure the backend's listening port and production CORS origins.
4.  Set the frontend's API base URL to the deployed backend URL using
    the project's supported environment-variable convention.
5.  Build and deploy the frontend.
6.  Test the deployed health endpoint, monitor creation/selection,
    manual checks, and check history.
7.  Verify scheduled checks continue running under the hosting
    platform's sleep, restart, and scheduling behavior.

**Deployment is not complete until these steps have been performed and
the live application has been tested.**

## Contributing

Contributions and suggestions are welcome.

1.  Fork the repository.
2.  Create a branch for your change.
3.  Make the change and add or update tests.
4.  Run the backend tests and frontend build.
5.  Commit the changes and open a pull request.

## Author

**Vinoth**

Project repository: <https://github.com/vino1408/Up_time_monitory>
