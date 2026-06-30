# Ticket Management Service (`ms-tickets`)

An event-driven, fully reactive microservice built with Spring Boot, Project Reactor, and RabbitMQ. It consumes processed payment events, securely persists ticket records and idempotency tracking within reactive transactions, and dispatches downstream notification requests.

## Key Features

* **End-to-End Reactive Pipeline:** Built entirely using Project Reactor (`Mono`), leveraging non-blocking execution chains.
* **Transactional Idempotency Protection:** Guards database operations using a reactive `TransactionalOperator`. It utilizes an `IdempotencyKeyEntity` with a 7-day sliding expiration to guarantee exact-once processing of tickets and eliminate duplicate customer notifications.
* **Asynchronous Notification Egress:** Automatically transforms finalized payments into standard notification payloads (`NotificationDTO`) and dispatches them asynchronously over a RabbitMQ exchange.
* **Listener-Level Input Validation:** Enforces strict contract structures on broker listeners via Jakarta Validation (`@Valid`) wired into a custom `RabbitListenerConfigurer` factory.
* **Resilient Infrastructure Architecture:** Implements fail-safe mechanisms including message Time-To-Live (TTL) and localized Dead Letter Queues (DLQ) to handle unparseable or toxic payloads without causing system blocking.

---

## Prerequisites

* Java 21
* RabbitMQ Broker
* PostgreSQL (with R2DBC reactive driver compatibility)
* Redis Cache
* Docker & Docker Compose

---

## Environment Configuration

Create a `.env` file in the root directory using the following template:

```bash
# --- Keycloak Configs --- 
KEYCLOAK_HOST=localhost

# --- Loki Log Aggregation Configs --- 
LOKI_HOST=localhost

# --- Reactive Postgres (R2DBC) Configs --- 
POSTGRES_DB=cinema_tickets
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=YOUR_SECURE_PASSWORD
POSTGRES_USER=YOUR_DB_USER

# --- Redis Cache Configs --- 
REDIS_HOST=localhost
REDIS_PASSWORD=YOUR_REDIS_PASSWORD

# --- RabbitMQ Broker Configs --- 
RABBITMQ_HOST=localhost
RABBITMQ_PASSWORD=YOUR_RABBITMQ_PASSWORD
RABBITMQ_USER=YOUR_RABBITMQ_USER

```

---

## Messaging Architecture

This microservice maps infrastructure settings dynamically under the `tickets.rabbitmq` configuration namespace.

### Queue Configurations

| Component | Target Property Binding | Core Purpose |
| --- | --- | --- |
| **Inbound Exchange** | `tickets.rabbitmq.payment-properties.exchangeName` | Topic Exchange collecting upstream payment outcomes. |
| **Main Processing Queue** | `tickets.rabbitmq.payment-properties.queueName` | Durable queue holding inbound payments for processing. |
| **DLQ Routing Topology** | `tickets.rabbitmq.payment-properties.dlqExchangeName` | Target exchange isolated for routing unrecoverable processing failures. |
| **Dead Letter Queue (DLQ)** | `tickets.rabbitmq.payment-properties.dlqName` | Dead letter repository keeping rejected payloads for inspection. |

---

## Event Schemas & Contracts

### 1. Inbound Event: Consumed Payment (`PaymentDTO`)

* **Queue Name:** Derived from `${tickets.rabbitmq.payment-properties.queueName}`
* **Schema Contract:**

```json
{
  "paymentId": "15dc1c3d-375a-4305-b1cf-cc9f0683301c",
  "date": "2026-06-29T12:12:55",
  "customerEmail": "customer@example.com",
  "bookingId": "175dcb06-8b78-4733-8054-57ce60445003",
  "amount": 19.99,
  "currency": "USD",
  "status": "COMPLETED",
  "paymentMethod": "Credit Card"
}

```

### 2. Outbound Event: Produced Notification (`NotificationDTO`)

* **Destination Exchange:** Configured via `tickets.rabbitmq.notification-properties.exchangeName`
* **Schema Contract:**

```json
{
  "notificationId": "8f2b7a11-094c-4122-8611-e230cb25f541",
  "notificationType": "PAYMENT",
  "subject": "Payment confirmation.",
  "provider": "GOOGLE",
  "recipient": "customer@example.com",
  "templateModel": {
    "paymentId": "15dc1c3d-375a-4305-b1cf-cc9f0683301c",
    "paymentDate": "2026-06-29T12:12:55",
    "productName": "TEMPORARY TEST",
    "amount": 19.99,
    "currencyType": "USD",
    "status": "COMPLETED",
    "paymentMethod": "Credit Card"
  }
}

```

---

## Idempotency Flow & Mechanics

To enforce consistency across decentralized event invocations, the message processing routine applies an explicit defensive workflow:

1. **Idempotency Lookup:** The worker checks for an existing `IdempotencyKeyEntity` matched against the event's `paymentId`.
2. **Duplicate Detection:** If a record matches, processing is safely bypassed, logging a message while suppressing redundant tasks and duplicate outbound notifications.
3. **Execution Path:** When no key exists, a new ticket entity and idempotency token are transactionally written. The system then triggers the message dispatcher to issue a notification downstream if the payment status is marked `COMPLETED`.

---

## Resiliency & Error Handling

### Structural Payload Rejection

If an inbound event violates field contracts (e.g., missing properties like `paymentId` or an empty `customerEmail`), the integrated `LocalValidatorFactoryBean` stops processing at the listener interface. The bad message is routed away from regular queues and moved to the DLQ to avoid delivery loops.

### Standard Exception Mapping

Internal exceptions or infrastructure issues are normalized into an explicit application model:

* **`VALIDATION_ERROR`**: Triggered by structural violations on the inbound payload.
* **`TECHNICAL_ERROR`**: Issued during template compilation, data transformations, or external connection outages.

Standardized response format for downstream visibility:

```json
{
  "message": "DB error during read",
  "code": "TECHNICAL_ERROR",
  "status": "INTERNAL_SERVER_ERROR"
}

```