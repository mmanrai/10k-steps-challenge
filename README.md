# 10k steps challenge application

The application is based on a set of event-driven micro-services.

The application described would work as follows:

    Users sport connected pedometers that track how many steps they take.

    The pedometers regularly send step-count updates to the application that manages the challenge.

    The goal is to walk at least 10,000 steps each day, and users are greeted by an email every day when they do so.

    Users may choose to be publicly listed in rankings of step counts over the last 24 hours.

    Participants can also connect to a web application to see their data and update their information, such as their city and whether they want to appear in public rankings.

The application is decomposed as a set of (micro) services that interact with each other as in figure 7.5. Each service fulfills a single functional purpose and could well be used by another application. There are four public services: two user-facing web applications, one service for receiving pedometer device updates, and one service to expose a public HTTP API. The public API is used by the user web application, and we could similarly have mobile applications connect to it. There are four internal services: one to manage user profiles, one to manage activity data, one to congratulate users over email, and one to compute various stats over continuous events.

Note You may have heard of command query responsibility segregation (CQRS) and event-sourcing, which are patterns found in event-driven architectures.1 CQRS structures how to read and write information, while event sourcing is about materializing the application state as a sequence of facts. Our proposed application architecture relates to both notions, but because it’s not strictly faithful to the definitions, I prefer to just call it an “event-driven microservices architecture.”

All services are powered by Vert.x, and we also need some third-party middleware, labelled “infrastructure services” in figure 7.5. We’ll use two different types of databases: a document-oriented database (MongoDB) and a relational database (PostgreSQL). We need an SMTP server to send emails, and Apache Kafka is used for event-stream processing between some services. Because the ingestion service may receive updates from HTTP and AMQP, we’ll also use an ActiveMQ Artemis server.

It all starts with a pedometer sending an update to the ingestion service, which verifies that the update contains all required data. The ingestion service then sends the update to a Kafka topic, and the pedometer device is acknowledged so it knows that the update has been received and will be processed. The update will be handled by multiple consumers listening on that particular Kafka topic, and among them is the activity service. This service will record the data to the PostgreSQL database and then publish another record to a Kafka topic with the number of steps recorded by the pedometer on that day. This record is picked up by the event stats service, which observes updates over windows of five seconds, splits them by city, and aggregates the number of steps. It then posts an update with the increment in steps observed for a given city as another Kafka record. This record is then consumed by the dashboard web application, which finally sends an update to all connected web browsers, which in turn update the display.
## Building the project

You need _Docker_ to be up and running in order to build and run the project.
Make sure that you have sufficient user permissions to create and start container images with _Docker_.

To build all services run:

    ./gradlew build

Tests are being run as part of the build process, relying on container images to be started to run integration tests against various middleware such as _Apache Kafka_ or _PostgreSQL_.
You may encounter occasional _flaky_ tests, and/or you may also not have enough resources to run all containers while running tests, which may explain potential errors.

To build all services without running tests simply run:

    ./gradlew assemble

## Running the project

First open a terminal to start all middleware containers with _Docker Compose_:

    docker-compose up

The micro-services are specified in a `Procfile` and there exist many tools to run them:

* in the book I recommend [foreman](https://github.com/ddollar/foreman) which is written in Ruby,
* you can alternatively use [hivemind](https://github.com/DarthSim/hivemind) which is written in Go and thus is available as a zero-dependency executable,
* you can find another tool, but `foreman` and `hivemind` are those that I have personally tested.

Open another terminal to run the services.
With `hivemind`:

    hivemind

or with `foreman`:

    foreman start

The services should now start.
There are 2 web applications you can use with a web browser:

* the user web application is at http://127.0.0.1:8080
* the dashboard is at http://127.0.0.1:8081

You can interact with the other HTTP services on ports 3000, 3001 and 4000.
