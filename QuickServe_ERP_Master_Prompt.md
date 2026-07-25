# QuickServe ERP — Master Vibe-Coding Prompt
## "SAP for Small Business" — Java Spring Boot Full Stack

---

## CONTEXT (Read this before every session)

You are building **QuickServe ERP** — a modular, multi-tenant, production-grade business
management platform for Indian SMBs (restaurants, cafes, retail stores, QSR chains,
franchise businesses). Think of it as a lightweight SAP/Odoo but designed for businesses
with 1–50 employees, built to run on a single ₹800/month VPS and scale to Kubernetes later.

The goal is **one unified platform** that replaces:
- Tally (accounting/GST) + Petpooja/Posist (POS) + Excel (HR) + separate inventory software

**Tech Stack (Non-negotiable):**
- Backend: Java 21 + Spring Boot 3.3 + Spring Security + Spring Data JPA
- Database: PostgreSQL 16 (primary) + Redis 7 (cache/sessions) + Elasticsearch 8 (search/audit)
- Messaging: Apache Kafka (async events between modules)
- Frontend: React 18 + TypeScript + Vite + TailwindCSS v4 + TanStack Query v5
- File Storage: MinIO (self-hosted S3-compatible, free) or AWS S3
- Infrastructure: Docker Compose (dev/VPS) → Kubernetes (scale)
- Reverse Proxy: Nginx + Let's Encrypt (free SSL)
- Monitoring: Prometheus + Grafana + Loki

**Architecture Pattern:** Modular Monolith first (not microservices — deploy as one JAR,
organized as modules internally). Migrate modules to microservices only when scale demands.
This is the pragmatic choice for a solo/small team building for VPS hosting.

**Multi-tenancy model:** Row-level isolation. Every database table has `business_id UUID`
and `outlet_id UUID` columns. No schema-per-tenant (too expensive on VPS). Use Hibernate
filters to auto-apply tenant context on every query.

**Indian Market Requirements:**
- GST tax engine (0%, 5%, 12%, 18%, 28% slabs) with HSN/SAC code support
- e-Invoice generation (IRN) via GST IRP API integration
- GSTR-1 and GSTR-3B report export (Excel/JSON)
- Rupee (₹) as primary currency, multi-currency support
- Razorpay for payments (UPI, Card, NetBanking, Wallet)
- WhatsApp Business API for customer notifications
- PF, ESI, TDS deductions in payroll

---

## COMPLETE MODULE SPECIFICATION

### MODULE 1: Auth & Identity (Foundation — Build First)

**Entities:**
```java
// SuperAdmin → BusinessOwner → OutletManager → Staff
User { id, name, email, phone, passwordHash, role, businessId, outletId, 
       isActive, lastLogin, createdAt }
Business { id, name, gstin, pan, address, logoUrl, currencyCode, 
           timezone, subscriptionPlan, subscriptionExpiry, modules[] }
Outlet { id, businessId, name, address, phone, type(RESTAURANT/RETAIL/QSR),
         gstNumber, latitude, longitude, isActive }
Role { id, name, permissions[] }  // SUPER_ADMIN, BUSINESS_OWNER, OUTLET_MANAGER, 
                                   // CASHIER, WAITER, KITCHEN_STAFF, ACCOUNTANT, HR_MANAGER
```

**API Endpoints:**
```
POST   /api/auth/register-business     → Onboard new business
POST   /api/auth/login                 → Returns JWT (HTTP-only cookie)
POST   /api/auth/refresh               → Refresh token rotation
POST   /api/auth/logout
GET    /api/auth/me
POST   /api/auth/forgot-password
POST   /api/auth/reset-password

GET    /api/businesses/{id}
PUT    /api/businesses/{id}
POST   /api/businesses/{id}/outlets
GET    /api/businesses/{id}/outlets
POST   /api/businesses/{id}/staff      → Create staff account
GET    /api/businesses/{id}/staff
PUT    /api/staff/{id}/toggle-active
PUT    /api/staff/{id}/reset-password
```

**Spring Security Config:**
- JWT filter chain with HTTP-only cookie extraction
- Method-level security: @PreAuthorize("hasRole('BUSINESS_OWNER')")
- Custom UserDetailsService loading from DB
- Hibernate multi-tenancy filter: auto-inject businessId on all JPA queries

---

### MODULE 2: Menu & Product Catalog

**Entities:**
```java
Category { id, businessId, outletId, name, imageUrl, sortOrder, isActive }
MenuItem { id, businessId, outletId, categoryId, name, description, 
           basePrice, taxSlabId, imageUrl, isVeg, isAvailable,
           preparationTime, calories, isArchived }
MenuItemVariant { id, menuItemId, name, priceModifier }  // Small/Medium/Large
MenuItemAddon { id, menuItemId, name, price, isRequired }  // Extra cheese, etc.
TaxSlab { id, businessId, name, percentage, hsnCode, sacCode }
// Default tax slabs seeded: 0%, 5%, 12%, 18%, 28%
```

**API Endpoints:**
```
GET    /api/menu/public/{outletId}          → Public endpoint for QR customers (no auth)
GET    /api/menu/categories
POST   /api/menu/categories
PUT    /api/menu/categories/{id}
DELETE /api/menu/categories/{id}
GET    /api/menu/items
POST   /api/menu/items                      → With image upload (Cloudinary/MinIO)
PUT    /api/menu/items/{id}
PATCH  /api/menu/items/{id}/availability
POST   /api/menu/items/bulk-import          → CSV import for large menus
GET    /api/menu/items/low-stock-linked     → Items linked to low-stock ingredients
```

---

### MODULE 3: Order Management (POS + QR + KDS)

**Entities:**
```java
Order { id, businessId, outletId, orderNumber, type(DINE_IN/TAKEAWAY/DELIVERY/QR_SELF),
        tableId, customerId, staffId, status(DRAFT/PLACED/PREPARING/READY/DELIVERED/CANCELLED),
        subtotal, taxAmount, discountAmount, serviceCharge, total, 
        paymentStatus(PENDING/PARTIAL/PAID), notes, createdAt, updatedAt }

OrderItem { id, orderId, menuItemId, menuItemName, quantity, unitPrice,
            variantId, variantName, addons[], taxAmount, totalPrice, 
            kdsStatus(PENDING/PREPARING/DONE), kdsNotes }

Payment { id, orderId, amount, method(CASH/CARD/UPI/RAZORPAY/CREDIT),
          transactionId, razorpayOrderId, razorpayPaymentId, 
          status(PENDING/SUCCESS/FAILED/REFUNDED), paidAt }

Table { id, outletId, name, capacity, status(AVAILABLE/OCCUPIED/RESERVED/CLEANING),
        qrCodeUrl, sectionId }
TableSection { id, outletId, name }  // Indoor, Outdoor, Rooftop
TableReservation { id, outletId, tableId, customerId, guestName, guestPhone,
                   partySize, reservedFor, duration, status, notes }
```

**API Endpoints:**
```
// POS
POST   /api/orders                    → Create order (POS or QR)
GET    /api/orders/{id}
PUT    /api/orders/{id}/items         → Update cart
PATCH  /api/orders/{id}/status
POST   /api/orders/{id}/payment       → Process payment
POST   /api/orders/{id}/split-bill    → Split bill between customers
POST   /api/orders/{id}/apply-discount
GET    /api/orders/{id}/invoice       → Generate PDF invoice
POST   /api/orders/{id}/cancel

// KDS (Kitchen Display)
GET    /api/kds/orders                → WebSocket stream OR polling endpoint
PATCH  /api/kds/items/{id}/status     → Update item KDS status
GET    /api/kds/display/{outletId}    → All active orders for kitchen

// Tables
GET    /api/tables
POST   /api/tables
PATCH  /api/tables/{id}/status
GET    /api/tables/{id}/current-order
POST   /api/tables/reservations
GET    /api/tables/reservations
PATCH  /api/tables/reservations/{id}/status

// QR Public (No Auth)
GET    /api/public/menu/{outletId}
POST   /api/public/orders             → Customer self-order
GET    /api/public/orders/{id}/track  → Live order tracking
POST   /api/public/orders/{id}/pay    → Razorpay payment initiation
```

**Kafka Events Published:**
- `order.placed` → triggers Inventory deduction
- `order.status.changed` → triggers KDS update, WhatsApp notification
- `payment.processed` → triggers Finance module journal entry

**WebSocket:**
- `/ws/kds/{outletId}` — real-time order push to kitchen display
- `/ws/order/{orderId}` — customer order status tracking

---

### MODULE 4: Finance & Accounting (Core ERP Differentiator)

**Entities:**
```java
// Double-entry bookkeeping
Account { id, businessId, code, name, type(ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE),
          parentId, balance, isSystem }
// System accounts seeded: Cash, Bank, Sales Revenue, CGST Payable, SGST Payable,
// Accounts Receivable, Accounts Payable, Stock Asset, Staff Expense

JournalEntry { id, businessId, reference, description, entryDate, 
               totalDebit, totalCredit, type, sourceModule, sourceId }
JournalLine { id, journalId, accountId, debit, credit, description }

Invoice { id, businessId, outletId, invoiceNumber, customerId, orderId,
          type(SALES/PURCHASE/CREDIT_NOTE/DEBIT_NOTE), 
          status(DRAFT/SENT/PAID/OVERDUE/CANCELLED),
          issueDate, dueDate, subtotal, cgst, sgst, igst, total,
          irn, qrCode, eInvoiceStatus, pdfUrl }

InvoiceLineItem { id, invoiceId, description, hsnCode, quantity, unitPrice,
                  taxSlabId, taxableAmount, cgst, sgst, igst, total }

Expense { id, businessId, outletId, category, description, amount, 
          taxAmount, vendorId, receiptUrl, date, approvedBy }

BankAccount { id, businessId, accountNumber, ifsc, bankName, balance }
BankTransaction { id, bankAccountId, type(CREDIT/DEBIT), amount, description,
                  reference, transactionDate, reconciledWith }
```

**API Endpoints:**
```
// Invoices
GET    /api/finance/invoices
POST   /api/finance/invoices          → Create with auto GST calculation
GET    /api/finance/invoices/{id}/pdf → Generate PDF
POST   /api/finance/invoices/{id}/send-whatsapp
POST   /api/finance/invoices/{id}/einvoice  → Generate IRN via IRP

// GST Reports
GET    /api/finance/gst/gstr1?month=2024-01    → GSTR-1 data
GET    /api/finance/gst/gstr3b?month=2024-01   → GSTR-3B summary
GET    /api/finance/gst/export?type=GSTR1&month=2024-01  → Excel download

// Accounts & Ledger
GET    /api/finance/accounts
GET    /api/finance/ledger/{accountId}?from=&to=
GET    /api/finance/trial-balance?date=
GET    /api/finance/profit-loss?from=&to=
GET    /api/finance/balance-sheet?date=

// Expenses
GET    /api/finance/expenses
POST   /api/finance/expenses
GET    /api/finance/expenses/categories

// Bank
GET    /api/finance/banks
POST   /api/finance/banks
POST   /api/finance/banks/{id}/transactions
GET    /api/finance/banks/{id}/statement
POST   /api/finance/banks/{id}/reconcile
```

**Auto-Journal Entry Logic (Kafka Consumer):**
```
ON order.payment.processed:
  DR Cash/Bank/Razorpay Clearing     (payment amount)
  CR Sales Revenue                   (subtotal)
  CR CGST Payable                    (cgst amount)
  CR SGST Payable                    (sgst amount)

ON purchase.received:
  DR Stock Asset                     (item value)
  DR Input CGST                      (cgst)
  CR Accounts Payable                (total)
```

---

### MODULE 5: Inventory & Supply Chain

**Entities:**
```java
InventoryItem { id, businessId, outletId, name, sku, barcode, unit, 
                category, currentStock, minStockLevel, maxStockLevel,
                reorderPoint, costPrice, lastPurchasePrice, 
                supplierId, isActive }

StockTransaction { id, outletId, itemId, type(IN/OUT/ADJUSTMENT/TRANSFER/WASTE),
                   quantity, balanceBefore, balanceAfter, reference, 
                   reason, notes, performedBy, transactionAt }

Supplier { id, businessId, name, contactPerson, phone, email, 
           gstin, address, paymentTerms, rating }

PurchaseOrder { id, businessId, outletId, poNumber, supplierId, 
                status(DRAFT/SENT/PARTIAL/RECEIVED/CANCELLED),
                expectedDelivery, subtotal, tax, total, notes }

POItem { id, poId, itemId, orderedQty, receivedQty, unitPrice, taxAmount }

GoodsReceivedNote { id, businessId, poId, supplierId, grnNumber,
                     receivedAt, invoiceNumber, notes }
GRNItem { id, grnId, poItemId, receivedQty, qualityStatus }

// Recipe/BOM — for restaurants
Recipe { id, businessId, menuItemId, name }
RecipeIngredient { id, recipeId, inventoryItemId, quantity, unit }
// When order placed → auto-deduct ingredients based on recipe
```

**API Endpoints:**
```
GET    /api/inventory/items
POST   /api/inventory/items
PUT    /api/inventory/items/{id}
GET    /api/inventory/items/low-stock    → Alert endpoint
GET    /api/inventory/items/{id}/history

POST   /api/inventory/adjust             → Manual stock adjustment
POST   /api/inventory/transfer           → Transfer between outlets

GET    /api/inventory/suppliers
POST   /api/inventory/suppliers

GET    /api/inventory/purchase-orders
POST   /api/inventory/purchase-orders
POST   /api/inventory/purchase-orders/{id}/send
POST   /api/inventory/purchase-orders/{id}/grn    → Receive goods

GET    /api/inventory/valuation          → FIFO stock valuation report
GET    /api/inventory/movement-report
```

---

### MODULE 6: HR & Payroll

**Entities:**
```java
Employee { id, businessId, outletId, employeeCode, name, phone, email,
           designation, department, dateOfJoining, dateOfBirth,
           panNumber, aadhaarNumber, bankAccount, ifsc,
           pfNumber, esiNumber, employmentType(FULL_TIME/PART_TIME/CONTRACT),
           salary, salaryType(MONTHLY/DAILY/HOURLY), status }

Shift { id, outletId, name, startTime, endTime, breakDuration }
ShiftAssignment { id, employeeId, shiftId, date }

Attendance { id, employeeId, date, checkIn, checkOut, 
             workHours, overtimeHours, status(PRESENT/ABSENT/HALF_DAY/LEAVE) }

LeaveType { id, businessId, name, daysAllowed, isPaid }
LeaveApplication { id, employeeId, leaveTypeId, fromDate, toDate,
                   reason, status(PENDING/APPROVED/REJECTED), approvedBy }

Payroll { id, businessId, month, year, status(DRAFT/PROCESSED/PAID) }
PayrollSlip { id, payrollId, employeeId, 
              // Earnings
              basicSalary, hra, conveyanceAllowance, otherAllowances, overtimePay,
              // Deductions  
              pfEmployee, esiEmployee, tds, professionalTax, otherDeductions,
              // Computed
              grossSalary, totalDeductions, netSalary,
              workingDays, presentDays, leaveDays,
              pdfUrl, paidAt }
```

**API Endpoints:**
```
GET    /api/hr/employees
POST   /api/hr/employees
PUT    /api/hr/employees/{id}
GET    /api/hr/employees/{id}/payslips

POST   /api/hr/attendance/check-in         → App-based check-in
POST   /api/hr/attendance/check-out
GET    /api/hr/attendance?employee=&date=
POST   /api/hr/attendance/bulk-import      → CSV import

GET    /api/hr/shifts
POST   /api/hr/shifts
POST   /api/hr/shifts/assign

GET    /api/hr/leaves
POST   /api/hr/leaves/apply
PATCH  /api/hr/leaves/{id}/approve
PATCH  /api/hr/leaves/{id}/reject

POST   /api/hr/payroll/process?month=&year=   → Auto-compute all payslips
GET    /api/hr/payroll/{id}/slips
GET    /api/hr/payroll/slips/{id}/pdf
POST   /api/hr/payroll/{id}/mark-paid
```

---

### MODULE 7: CRM & Loyalty

**Entities:**
```java
Customer { id, businessId, name, phone, email, address, 
           dateOfBirth, anniversary, loyaltyPoints, tier(BRONZE/SILVER/GOLD/PLATINUM),
           totalSpend, visitCount, lastVisitAt, tags[], notes }

LoyaltyProgram { id, businessId, name, pointsPerRupee, redemptionRate,
                 tiers[{name, minPoints, discount}] }

LoyaltyTransaction { id, customerId, type(EARNED/REDEEMED/EXPIRED),
                     points, orderId, description, transactionAt }

Campaign { id, businessId, name, type(BIRTHDAY/ANNIVERSARY/WINBACK/PROMOTIONAL),
           trigger, message, channel(WHATSAPP/SMS/EMAIL), status, scheduledAt }
```

**API Endpoints:**
```
GET    /api/crm/customers
POST   /api/crm/customers
GET    /api/crm/customers/search?q=        → Phone/name search at POS
GET    /api/crm/customers/{id}
GET    /api/crm/customers/{id}/order-history
GET    /api/crm/customers/{id}/loyalty

POST   /api/crm/loyalty/earn              → Award points on order
POST   /api/crm/loyalty/redeem            → Redeem at POS

GET    /api/crm/campaigns
POST   /api/crm/campaigns
POST   /api/crm/campaigns/{id}/send
```

---

### MODULE 8: Analytics & BI Dashboard

**API Endpoints (all return pre-aggregated data):**
```
GET /api/analytics/dashboard?from=&to=&outletId=
  Response: {
    revenue: { total, today, thisWeek, thisMonth, growth% },
    orders: { total, avgOrderValue, peakHour, cancelRate },
    topItems: [{ name, quantity, revenue }],
    paymentBreakdown: { cash, upi, card, online },
    tableUtilization: { avgTurnover, peakOccupancy },
    staffPerformance: [{ name, orders, revenue }]
  }

GET /api/analytics/sales-report?from=&to=&groupBy=DAY|WEEK|MONTH
GET /api/analytics/item-performance?from=&to=
GET /api/analytics/customer-insights   → RFM analysis
GET /api/analytics/inventory-analysis  → Fast/slow moving, wastage
GET /api/analytics/pl-summary?from=&to=    → Revenue - COGS - OpEx = Net Profit
GET /api/analytics/gst-summary?month=
GET /api/analytics/export?report=sales&format=EXCEL|PDF
```

---

### MODULE 9: SaaS Platform (Your Own Business Model)

This module is for YOU — it manages the businesses paying you subscription fees.

**Entities:**
```java
SubscriptionPlan { id, name, price, billingCycle(MONTHLY/ANNUAL), 
                   maxOutlets, maxStaff, modules[], features[] }
// Plans: Starter (₹999/mo, 1 outlet), Growth (₹2499/mo, 3 outlets), 
//        Enterprise (₹4999/mo, unlimited)

Subscription { id, businessId, planId, status(TRIAL/ACTIVE/SUSPENDED/CANCELLED),
               trialEndsAt, currentPeriodStart, currentPeriodEnd, 
               razorpaySubscriptionId }
```

**API Endpoints:**
```
GET  /api/platform/plans
POST /api/platform/subscribe
POST /api/platform/subscribe/webhook    → Razorpay subscription webhook

// SuperAdmin only
GET  /api/platform/admin/businesses
GET  /api/platform/admin/metrics        → MRR, churn, new signups
POST /api/platform/admin/impersonate    → Login as any business for support
```

---

## PROJECT STRUCTURE

```
quickserve-erp/
├── backend/
│   ├── src/main/java/com/quickserve/
│   │   ├── QuickServeApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   ├── KafkaConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   └── TenantConfig.java         ← Multi-tenancy Hibernate filter
│   │   ├── common/
│   │   │   ├── entity/BaseEntity.java    ← id, businessId, createdAt, updatedAt
│   │   │   ├── security/JwtFilter.java
│   │   │   ├── security/TenantContext.java
│   │   │   ├── exception/GlobalExceptionHandler.java
│   │   │   └── response/ApiResponse.java
│   │   ├── modules/
│   │   │   ├── auth/
│   │   │   ├── menu/
│   │   │   ├── order/
│   │   │   ├── finance/
│   │   │   ├── inventory/
│   │   │   ├── hr/
│   │   │   ├── crm/
│   │   │   ├── analytics/
│   │   │   └── platform/
│   │   └── integrations/
│   │       ├── razorpay/
│   │       ├── whatsapp/
│   │       ├── gst/              ← IRP e-invoice integration
│   │       └── storage/          ← MinIO/S3
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/         ← Flyway migrations (V1__init.sql, V2__menu.sql...)
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/                  ← Routes, layout
│   │   ├── modules/
│   │   │   ├── auth/
│   │   │   ├── pos/              ← POS interface
│   │   │   ├── kds/              ← Kitchen display
│   │   │   ├── finance/
│   │   │   ├── inventory/
│   │   │   ├── hr/
│   │   │   ├── crm/
│   │   │   └── analytics/
│   │   ├── customer/             ← QR self-order PWA (separate route group)
│   │   └── shared/               ← Components, hooks, API client
│   └── package.json
│
├── docker/
│   ├── docker-compose.yml        ← All services for local dev
│   ├── docker-compose.prod.yml   ← VPS production config
│   └── nginx/nginx.conf
│
└── scripts/
    ├── deploy.sh                 ← VPS deployment script
    └── seed.sql                  ← Demo data for onboarding
```

---

## DEPENDENCIES (pom.xml key dependencies)

```xml
<!-- Core -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-validation</dependency>
<dependency>spring-boot-starter-websocket</dependency>
<dependency>spring-boot-starter-data-redis</dependency>
<dependency>spring-boot-starter-actuator</dependency>

<!-- Database -->
<dependency>postgresql</dependency>
<dependency>flyway-core</dependency>

<!-- Kafka -->
<dependency>spring-kafka</dependency>

<!-- Security -->
<dependency>jjwt-api 0.12.x</dependency>
<dependency>jjwt-impl 0.12.x</dependency>

<!-- PDF Generation -->
<dependency>openpdf 2.0.x</dependency>  <!-- or jasperreports -->

<!-- Excel Export -->
<dependency>apache-poi 5.x</dependency>

<!-- HTTP Client (Razorpay, WhatsApp, IRP) -->
<dependency>spring-boot-starter-webflux</dependency>  <!-- WebClient -->

<!-- Elasticsearch -->
<dependency>spring-data-elasticsearch</dependency>

<!-- Utilities -->
<dependency>mapstruct 1.5.x</dependency>
<dependency>lombok</dependency>
<dependency>jackson-databind</dependency>
```

---

## VPS DEPLOYMENT (Free/Cheap Hosting Strategy)

### docker-compose.prod.yml (single VPS, everything on one machine)
```yaml
version: '3.8'
services:
  app:
    image: quickserve-backend:latest
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/quickserve
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      REDIS_URL: redis://redis:6379
    depends_on: [postgres, redis, kafka]

  postgres:
    image: postgres:16-alpine
    volumes: ["postgres_data:/var/lib/postgresql/data"]
    environment: { POSTGRES_DB: quickserve, POSTGRES_USER: ${DB_USERNAME}, POSTGRES_PASSWORD: ${DB_PASSWORD} }

  redis:
    image: redis:7-alpine
    volumes: ["redis_data:/data"]

  kafka:
    image: bitnami/kafka:latest
    environment: { KAFKA_CFG_NODE_ID: 1, KAFKA_CFG_PROCESS_ROLES: broker+controller, KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093 }

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    volumes: ["minio_data:/data"]
    environment: { MINIO_ROOT_USER: ${MINIO_USER}, MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD} }

  nginx:
    image: nginx:alpine
    ports: ["80:80", "443:443"]
    volumes: ["./nginx/nginx.conf:/etc/nginx/nginx.conf", "certbot_data:/etc/letsencrypt"]

  certbot:
    image: certbot/certbot
    volumes: ["certbot_data:/etc/letsencrypt"]

volumes:
  postgres_data:
  redis_data:
  minio_data:
  certbot_data:
```

### Recommended VPS Options (Ascending cost):
1. **Oracle Cloud Free Tier** — 4 vCPU ARM, 24GB RAM, completely free forever ← START HERE
2. **Hostinger VPS** — ₹800/month, 2 vCPU, 8GB RAM, good for 50 businesses
3. **DigitalOcean Droplet** — $12/month, 2 vCPU, 4GB RAM

---

## BUILD ORDER (Vibe-Coding Sequence)

**Week 1-2: Foundation**
1. Spring Boot project setup + Flyway migrations + BaseEntity
2. Multi-tenancy Hibernate filter (TenantContext ThreadLocal)
3. Auth module: register, login, JWT, RBAC
4. React setup: routing, auth context, axios interceptors

**Week 3-4: Revenue-Generating Core**
5. Menu module (CRUD + image upload to MinIO)
6. Order module (POS flow: cart → payment → invoice PDF)
7. QR self-order (public endpoints + Razorpay)
8. KDS (WebSocket push or polling)

**Week 5-6: ERP Differentiators**
9. Finance module (accounts, journal entries, invoice PDF)
10. GST tax engine + GSTR-1 export
11. Inventory module (stock tracking + PO + GRN)
12. Recipe/BOM for auto-deduction

**Week 7-8: People & Intelligence**
13. HR module (employees, attendance, shift)
14. Payroll (PF/ESI/TDS computation + payslip PDF)
15. Analytics dashboard (aggregated queries + charts)
16. CRM + Loyalty program

**Week 9-10: Production Hardening**
17. WhatsApp Business API notifications
18. e-Invoice (IRP API integration)
19. SaaS billing (Razorpay subscriptions)
20. Docker Compose prod + Nginx + SSL + monitoring

---

## COMMON VIBE-CODING PROMPTS TO USE PER MODULE

**Starting a module:**
> "I am building QuickServe ERP in Java 21 + Spring Boot 3.3 + PostgreSQL. 
> Using multi-tenant architecture with businessId on every entity.
> Now build the complete [MODULE NAME] module including:
> - JPA entities with proper relationships
> - Flyway migration SQL
> - Repository, Service, Controller
> - DTOs with MapStruct mappers
> - All REST endpoints from the spec
> - Unit tests for service layer
> Follow the project structure at [paste structure above]"

**For Kafka events:**
> "Add Kafka producer to OrderService that publishes 'order.placed' event
> and a Kafka consumer in InventoryService that deducts stock based on recipe ingredients"

**For PDF generation:**
> "Generate a GST-compliant tax invoice PDF using OpenPDF with:
> - Business logo, GSTIN, invoice number, IRN
> - Line items table with HSN code, qty, rate, CGST%, SGST%, amount
> - Summary: subtotal, CGST, SGST, grand total
> - Payment QR code (Razorpay)"

**For React frontend:**
> "Build the POS interface in React + TypeScript + TailwindCSS.
> Requirements: category sidebar, item grid, cart panel (right side),
> quantity/variant/addon selection modal, payment modal with method toggle (Cash/UPI/Card),
> print invoice button. Use TanStack Query for API calls."

---

## SECURITY CHECKLIST (Before Production)

- [ ] JWT secret is 256-bit random, stored in env var (not code)
- [ ] All financial endpoints require HTTPS
- [ ] SQL injection: using parameterized queries (JPA/Hibernate only, no native SQL strings)
- [ ] Tenant isolation verified: test that business A cannot access business B's data
- [ ] Rate limiting on auth endpoints (Spring Cloud Gateway or Bucket4j)
- [ ] File upload validation: only images/PDFs, max 5MB, scan for malware
- [ ] CORS configured for only your frontend domain
- [ ] Audit log for all financial transactions (who changed what, when)
- [ ] PII fields (Aadhaar, PAN) encrypted at rest (AES-256)
- [ ] Razorpay webhook signature verification

---

*QuickServe ERP — Modular. Multi-tenant. Made in India.*
