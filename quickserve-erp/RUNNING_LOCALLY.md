# QuickServe ERP — Running Locally

A step-by-step guide to run the **full stack** (Postgres + Redis + Kafka + MinIO + ES + backend + frontend), the **mock mode** (no external services, zero API keys), or just one layer at a time.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 21+ | [Adoptium Temurin 21](https://adoptium.net/) |
| Maven | 3.9+ | bundled `./mvnw` wrapper |
| Node.js | 22 LTS | [nodejs.org](https://nodejs.org/) |
| Docker + Compose | 26+ | [docs.docker.com](https://docs.docker.com/get-docker/) |
| Git | any | |

---

## Option A — Full Mock Mode (Fastest, No Docker, No API Keys)

Everything runs in-process. The backend uses a real PostgreSQL (you need Docker for just that), MSW intercepts all API calls in the browser so you don't even need the backend running.

### 1. Start Postgres + Redis only

```bash
cd quickserve-erp
docker compose -f docker/docker-compose.mock.yml up -d
```

### 2. Start the backend (mock profile)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

What mock profile does:
- OTPs are printed to the log — no Twilio needed
- Bypass OTP: use `123456` for any phone number
- Seeds 3 demo accounts automatically (see below)
- WhatsApp messages are logged, not sent
- Kafka / MinIO / Elasticsearch are completely disabled

Backend starts at **http://localhost:8080**

### 3. Start the frontend (MSW mock mode — no backend needed at all)

```bash
cd frontend
npm install
npx msw init public/ --save   # one-time: copies service worker file
npm run dev:mock
```

Frontend starts at **http://localhost:5173**

Open http://localhost:5173 — the MSW service worker intercepts all `/api/*` calls with realistic fake data. **No backend is needed** in this mode.

---

## Option B — Backend + Frontend with Real DB

Requires Docker for infrastructure only.

### 1. Start infrastructure

```bash
cd quickserve-erp
docker compose -f docker/docker-compose.yml up -d postgres redis kafka minio elasticsearch
```

### 2. Copy and fill `.env`

```bash
cp .env.template .env
# Edit .env — minimum required fields for dev:
# DB_PASSWORD, JWT_SECRET (any 32+ char string), MINIO_ROOT_PASSWORD
```

### 3. Start backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Start frontend (real API)

```bash
cd frontend
npm install
npm run dev
```

Frontend at **http://localhost:5173**, backend at **http://localhost:8080**

---

## Option C — Full Docker Compose (All Services)

```bash
cd quickserve-erp
cp .env.template .env
# fill in required values in .env
docker compose -f docker/docker-compose.yml up --build
```

Services:
| Service | Port |
|---------|------|
| Frontend (Nginx) | 80 |
| Backend (Spring Boot) | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| MinIO Console | 9001 |
| Elasticsearch | 9200 |

---

## Demo Credentials

These accounts are auto-seeded by `DemoDataSeeder` (mock profile) and `seed-demo.sql` (dev/prod):

| Phone | Password | Role |
|-------|----------|------|
| `9999999999` | `Demo@1234` | Business Owner |
| `8888888888` | `Demo@1234` | Cashier |
| `7777777777` | `Demo@1234` | Kitchen Staff |

**OTP bypass (mock profile):** enter `123456` for any OTP prompt.

---

## Environment Variables Reference

See [`.env.template`](.env.template) for all variables. Key ones:

| Variable | Description |
|----------|-------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | PostgreSQL connection |
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials |
| `JWT_SECRET` | HS256 signing key — min 32 chars |
| `JWT_EXPIRY_MS` | Token expiry in ms (default 86400000 = 24h) |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_PHONE` | SMS (optional in mock) |
| `META_WHATSAPP_TOKEN` / `META_PHONE_NUMBER_ID` | WhatsApp Business API (optional) |
| `MINIO_ENDPOINT` / `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Object storage (optional in mock) |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Online payments (optional) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka (optional in mock) |
| `REDIS_HOST` / `REDIS_PORT` | Redis |

---

## Running Tests

```bash
# Backend unit + integration tests (Testcontainers auto-starts Postgres + Redis)
cd backend
./mvnw test

# Frontend type-check
cd frontend
npm run typecheck

# Frontend lint
npm run lint
```

---

## Production Deployment (VPS)

1. Point your DNS A record to your server IP.
2. Fill in `.env` with production values.
3. Run:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

The script:
- Pulls latest code
- Builds Docker images
- Starts `docker-compose.prod.yml`
- Obtains TLS certificates via Certbot (Let's Encrypt)

---

## Project Structure

```
quickserve-erp/
├── backend/                   Spring Boot 3.3 + Java 21
│   ├── src/main/java/com/quickserve/
│   │   ├── common/            Shared: entities, security, utils, exceptions
│   │   ├── config/            Spring configs (Security, Kafka, Redis, MinIO, WS)
│   │   ├── integrations/      SMS, Storage, WhatsApp, Razorpay, GST adapters
│   │   ├── mock/              DemoDataSeeder, MockEventPublisher
│   │   └── modules/           Feature modules (auth, menu, order, kds, finance…)
│   └── src/main/resources/
│       ├── application.yml            Base config
│       ├── application-dev.yml        Dev overrides
│       ├── application-mock.yml       Zero-dependency mock profile
│       ├── application-prod.yml       Production hardening
│       └── db/migration/              Flyway V1–V8 SQL migrations
├── frontend/                  React 18 + TypeScript + Vite + TailwindCSS
│   └── src/
│       ├── mocks/             MSW handlers + fixture data (mock frontend mode)
│       ├── modules/           Feature pages (auth, pos, kds, menu, orders…)
│       └── shared/            API client, Zustand stores, layout
├── docker/                    Docker Compose files + Nginx config
└── scripts/                   deploy.sh, backup.sh, seed-demo.sql
```

---

## Module Quick Reference

| Module | Backend path | Frontend path |
|--------|-------------|---------------|
| Auth / Onboarding | `modules/auth`, `modules/onboarding` | `modules/auth`, `modules/onboarding` |
| Menu Management | `modules/menu` | `modules/menu` |
| POS / Orders | `modules/order` | `modules/pos`, `modules/orders` |
| Kitchen Display | `modules/kds` | `modules/kds` |
| Finance / GST | `modules/finance` | `modules/finance` |
| HR & Payroll | `modules/hr` | `modules/hr` |
| CRM / Loyalty | `modules/crm` | `modules/crm` |
| Inventory | `modules/inventory` | `modules/inventory` |
| Analytics | `modules/analytics` | `modules/analytics` |
| WhatsApp | `modules/whatsapp` | (via backend) |

---

## Troubleshooting

**`Error: Cannot find module './mocks/browser'` at runtime**
→ This only imports when `VITE_MOCK=true`. Use `npm run dev:mock`.

**MSW service worker not found (404 on `/mockServiceWorker.js`)**
→ Run `npx msw init public/ --save` once inside the `frontend/` directory.

**Backend fails with `No qualifying bean of type 'SmsService'`**
→ Make sure you're running with the `mock` profile: `./mvnw spring-boot:run -Dspring-boot.run.profiles=mock`

**Flyway migration fails**
→ Check `DB_PASSWORD` in your `.env` matches what Postgres was started with. For a clean slate: `docker volume rm quickserve-erp_pgdata`.

**Port 8080 already in use**
→ Set `SERVER_PORT=9090` in `.env` and update `vite.config.ts` proxy target accordingly.

---

*Made with ❤️ using Spring Boot 3.3, React 18, PostgreSQL 16, and Redis 7*
