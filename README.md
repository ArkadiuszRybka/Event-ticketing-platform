# 🎟️ Ticketing Platform — Spring Boot, PostgreSQL, Redis, JWT, Docker

Event Ticket Reservation System with seat reservations, optimistic locking, payments, order lifecycle, admin panel, monitoring and schedulers.

---

## 📌 Project Overview

A production-grade monolithic application for managing events, reserving seats (with HOLD/LOCK/SOLD lifecycle), processing payments, and handling order workflows.

**Key mechanics:**

- Distributed seat holding with TTL  
- Optimistic locking to prevent double booking seats  
- Idempotent order and webhook processing  
- Background schedulers for automatic hold expiration  
- Admin panel for managing events and orders  
- Actuator monitoring  
- Rate limiting  
- JWT security with roles (USER, ADMIN)  

---

## 🧭 Features

### 👤 User Functionality

- Register & Login (JWT)  
- View published events  
- Browse seat map for an event  
- Hold seats (temporary reservation, TTL e.g. 5 min)  
- Convert hold to order (LOCK → SOLD after payment)  
- View “My Orders”  
- Webhook-based payment confirmation  

### 🛠️ Admin Functionality

- Create / Edit / Delete venues (with seating layout)  
- Create events with state machine: DRAFT → PUBLISHED → CANCELLED  
- Automatic seat generation for each event based on venue layout  
- View all orders  
- Manual refunds  
- Event & order reporting  
- Manage users  
- System monitoring (Actuator)  
- Configure rate limits  

### 💳 Payment Processing (Mocked Stripe-like flow)

- Payment session creation (client receives payment URL)  
- Payment confirmation via webhook (idempotent)  
- Order lifecycle: HOLD → LOCKED → PAID/SOLD  
- Failed payments automatically release seats  
- Idempotent handling of retries & duplicated notifications

  **Security:**  
- JWT access tokens  
- Role-based access (USER / ADMIN)  
- Rate limiting on selected endpoints  
- Webhook signed secret validation  

**Concurrency & Consistency:**  
- Optimistic locking for seat updates  
- Idempotency keys for hold/order/payment processing  
- Distributed TTL for hold expiration  

**Redis usage:**  
- Idempotency storage  
- Caching  
- Rate limiting buckets  

---

## 🗄️ Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend   | Java 17, Spring Boot 3+ |
| Security  | Spring Security, JWT |
| Database  | PostgreSQL |
| Caching / Idempotency | Redis |
| Migrations | Flyway |
| Monitoring | Spring Actuator, Micrometer |
| Concurrency | Optimistic Locking |
| Scheduler | Spring Scheduling |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |

---

## 🚀 Installation & Setup

### 🔧 Prerequisites

- Docker 20.10+  
- Docker Compose 2.0+  
- JDK 17+  
- Redis installed locally OR running via Docker  

### 📁 Clone the project

```bash
git clone https://github.com/your-repo/ticketing-platform.git
cd ticketing-platform
```

## Create .env file
```bash
JWT_SECRET=your_jwt_secret_key

# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=ticketing

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Payment mock
PAYMENT_WEBHOOK_SECRET=your_webhook_secret
```

## Running with Docker
```bash
mvn clean package

docker-compose up --build -d
```

## 🧩 Summary
-Production-ready ticketing backend with:
-Authentication & authorization
-Event management
-Real-world seat reservation logic
-Concurrency control & idempotency
-Payment flow
-Admin interface
-Monitoring & rate limiting
-Schedulers
-Docker deployment

