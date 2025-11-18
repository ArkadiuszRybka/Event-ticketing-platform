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

- Register/Login (JWT)  
- View events & seat map  
- Hold seats (TTL 5min) → Convert to order  
- View orders  
- Webhook-based payment confirmation  

### 🛠️ Admin Functionality

- Manage venues & events (DRAFT → PUBLISHED → CANCELLED)  
- Automatic seat generation  
- View orders, manual refunds, reporting  
- User management & system monitoring  
- Rate limit configuration  

### 💳 Payment Processing (Mocked Stripe-like flow)

- Mocked Stripe-like flow  
- Idempotent webhook processing  
- Order lifecycle: HOLD → LOCKED → PAID/SOLD  

## 🧠 Architecture

- Security: JWT + role-based access  
- Concurrency: Optimistic locking & idempotency keys  
- Redis: caching, idempotency, rate limiting  
- Schedulers: auto-release holds, optional order expiration  
- Monitoring: Spring Actuator 

---

## 🗄️ Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend   | Java 17, Spring Boot 3+ |
| Security  | Spring Security, JWT |
| Database  | PostgreSQL |
| Caching / Idempotency | Redis |
| Migrations | Flyway |
| Monitoring | Spring Actuator |
| Concurrency | Optimistic Locking |
| Scheduler | Spring Scheduling |
| Containerization | Docker |
| Build Tool | Maven |

---

## 🚀 Installation & Setup

### 🔧 Prerequisites

- Docker 20.10+  
- Docker Compose 2.0+  
- JDK 17+  

### 📁 Clone the project

```bash
git clone https://github.com/ArkadiuszRybka/Event-ticketing-platform.git
cd Event-ticketing-platform
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
```
```bash
docker-compose up --build -d
```

## 🧩 Summary
- Production-ready ticketing backend with:
- Authentication & authorization
- Event management
- Real-world seat reservation logic
- Concurrency control & idempotency
- Payment flow
- Admin interface
- Monitoring & rate limiting
- Schedulers
- Docker deployment

