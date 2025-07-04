# 10k Steps Challenge Application

This project is a microservices-based application for tracking daily step counts using connected pedometers. Users can view their stats, update their profiles, and opt in to public rankings.

## Architecture Overview

- **Microservices**: Each service fulfills a single functional purpose and can be used independently.
- **Tech Stack**: [Vert.x](https://vertx.io/) for all services, MongoDB and PostgreSQL for data, Kafka for event streaming, ActiveMQ Artemis for messaging, and MailHog for email testing.
- **Infrastructure**: All required services (MongoDB, PostgreSQL, Kafka, MailHog, ActiveMQ) can be started with the provided `docker-compose.yml` at the project root.

## Features
- Track and update user step counts
- Daily congratulatory emails for reaching 10,000 steps
- Public rankings (opt-in)
- User profile management
- Real-time dashboard

## Getting Started

### Prerequisites
- [Docker](https://www.docker.com/) installed and running
- Java 21+ (for building/running services)

### 1. Start Infrastructure

From the project root, run:
```sh
docker-compose up
```
This will start MongoDB, PostgreSQL, Kafka, MailHog, and ActiveMQ Artemis.

### 2. Build All Services

```sh
./gradlew build
```

To build without running tests:
```sh
./gradlew assemble
```

### 3. Run the Microservices

Each service can be started individually. Example:
```sh
cd user-profile-service
./gradlew run
```
(Repeat for other services as needed.)

## Web Applications
- **User Web App**: [http://127.0.0.1:8080](http://127.0.0.1:8080)
- **Dashboard**: [http://127.0.0.1:8081](http://127.0.0.1:8081)

## API Endpoints
- User profile service: `http://localhost:3000`
- Other services: See respective service documentation

## Notes
- All infrastructure services are managed via `docker-compose.yml`.
- Use [MongoDB Compass](https://www.mongodb.com/try/download/compass) or similar tools to inspect databases.
- MailHog UI for email testing: [http://localhost:8025](http://localhost:8025)

---

**Happy walking!**
