# QuickServe ERP — Onboarding Flow & User Journey
## Priority: Build this FIRST. Easy onboarding = more paying customers.

---

## Philosophy

A business owner should go from "sign up" to "first order taken" in under 10 minutes.
The onboarding wizard is a 5-step guided setup that collects only what is needed to start,
defers everything else to settings later, and ends with a working demo environment.

Every step must be saveable as a draft — if the owner closes the browser, they resume
where they left off. Progress is stored server-side against the businessId.

---

## Step-by-Step Onboarding Wizard

### Step 0 — Landing / Register
**URL:** `/register`
**What happens:**
- Owner enters: Business Name, Owner Name, Mobile (10-digit), Email, Password
- Mobile OTP verification via Twilio SMS (or WhatsApp OTP as fallback)
- Email verification link via Resend
- System creates: Business record + BUSINESS_OWNER user + default 14-day trial subscription
- Redirects to Step 1

**WhatsApp moment:** After OTP verified, send WhatsApp message:
```
Welcome to QuickServe, [Name]! 🎉
Your business "[Business Name]" is being set up.
Complete your setup at: [link]
Your trial ends on: [date]
```

**Vibe-coding prompt for this step:**
```
Build the registration endpoint for QuickServe ERP.

POST /api/auth/register

Request body:
{
  "businessName": "Sharma Cafe",
  "ownerName": "Rajesh Sharma",
  "mobile": "9876543210",
  "email": "rajesh@sharmacafe.com",
  "password": "SecurePass123!"
}

Requirements:
1. Validate mobile is 10 digits, email format, password min 8 chars with 1 uppercase 1 number
2. Check mobile and email not already registered (return specific error messages)
3. Hash password with BCrypt strength 12
4. Create Business entity with status=ONBOARDING, plan=TRIAL, trialEndsAt=now+14days
5. Create User entity with role=BUSINESS_OWNER linked to business
6. Generate 6-digit OTP, store in Redis with key "otp:{mobile}" TTL 10 minutes
7. Send OTP via Twilio SMS
8. Send WhatsApp welcome message via Meta Cloud API
9. Return 201 with businessId and userId (no JWT yet — user must verify OTP first)
10. Write JUnit test covering: success case, duplicate mobile, duplicate email, invalid password

Also build:
POST /api/auth/verify-otp  → verifies OTP, issues JWT (HTTP-only cookie), marks mobile verified
POST /api/auth/resend-otp  → rate limited: max 3 resends per mobile per hour (Bucket4j + Redis)
```

---

### Step 1 — Business Profile
**URL:** `/onboarding/business`
**What the owner fills:**
- Business Type: Restaurant / Cafe / QSR / Retail / Bakery / Other
- GSTIN (optional at this step — can add later)
- PAN Number (optional)
- Business Address (Street, City, State, PIN)
- Logo Upload → stored in MinIO, URL saved to Business.logoUrl
- Primary Currency: INR (default, locked for Indian businesses)
- Timezone: Asia/Kolkata (default)
- Tax preference: GST inclusive or exclusive pricing

**Vibe-coding prompt:**
```
Build the business profile update endpoint.

PUT /api/onboarding/business-profile

This is Step 1 of the onboarding wizard. The user is already authenticated (JWT cookie).

Entities to update: Business (existing record created at registration)

Fields:
- businessType: enum (RESTAURANT, CAFE, QSR, RETAIL, BAKERY, FRANCHISE, OTHER)
- gstin: String (optional, validate format: 15-char alphanumeric if provided)
- pan: String (optional, validate format: AAAAA9999A)
- addressLine1, addressLine2, city, state, pincode
- logoUrl: String (set after MinIO upload via separate endpoint)
- gstInclusive: boolean (if true, item prices include GST; if false, GST added on top)

Also build:
POST /api/onboarding/upload-logo
  - Accept multipart file, max 2MB, types: jpg/png/webp only
  - Upload to MinIO bucket "business-assets/{businessId}/logo.{ext}"
  - Return presigned URL valid 1 year
  - Update Business.logoUrl

GET /api/onboarding/progress
  - Return { currentStep: 1, completedSteps: [0], totalSteps: 5, percentComplete: 20 }
  - Frontend uses this to resume interrupted onboarding

Write complete Flyway migration for any schema changes.
Write service + controller + DTO + MapStruct mapper.
Write test: valid GSTIN, invalid GSTIN format, logo upload size exceeded.
```

---

### Step 2 — Outlet Setup
**URL:** `/onboarding/outlet`
**What the owner fills:**
- Outlet Name (e.g., "Sharma Cafe - MG Road Branch")
- Outlet Type: Dine-In / Takeaway / Both / Delivery Only
- Outlet Address (can copy from business address)
- Outlet Phone
- Number of tables (if dine-in) — system auto-creates Table records named T1, T2...T{n}
- Operating hours: Open/Close time per day of week
- Outlet GST number (if different from main business)

**Note:** First outlet is created automatically. Owner can add more outlets later from settings.

**Vibe-coding prompt:**
```
Build the outlet creation flow for onboarding Step 2.

POST /api/onboarding/outlet

Request:
{
  "name": "Sharma Cafe - MG Road",
  "type": "DINE_IN",
  "phone": "9876543210",
  "address": { "line1": "...", "city": "Bengaluru", "state": "Karnataka", "pin": "560001" },
  "tableCount": 12,
  "operatingHours": [
    { "day": "MONDAY", "openTime": "09:00", "closeTime": "23:00", "isClosed": false },
    ...all 7 days
  ],
  "gstNumber": "29AABCS1429B1Z1"
}

Requirements:
1. Create Outlet entity linked to business
2. If tableCount > 0: auto-create Table records named T1...T{n}, all status=AVAILABLE
3. Generate unique QR code URL for each table: /qr/{outletId}/{tableId}
4. Create QR code PNG using ZXing library, upload to MinIO at tables/{outletId}/{tableId}.png
5. Seed default tax slabs for this outlet: 0%, 5%, 12%, 18%, 28% GST
6. Seed default menu categories: Starters, Main Course, Beverages, Desserts (for restaurant type)
   OR: Electronics, Clothing, Grocery, Others (for retail type) — based on businessType
7. Mark onboarding step 2 complete

WhatsApp notification after outlet created:
"Your outlet [name] is ready! 
Tables created: [count]
QR codes are ready to print.
Next: Add your menu items at [link]"

Write all entities, migration, service, controller, tests.
Special test: tableCount=0 should not create any Table records.
Special test: tableCount=50 should create exactly 50 tables.
```

---

### Step 3 — Menu / Product Catalog Setup
**URL:** `/onboarding/menu`
**What the owner does:**
- 3 options presented:
  a. Quick Add — fill a simple form to add items one by one (fastest)
  b. Bulk Import — download Excel template, fill it, upload it
  c. Skip for now — use demo items (5 sample items added automatically)
- For each item: Name, Category, Price, Tax Slab, Veg/Non-Veg toggle, Image (optional)

**Vibe-coding prompt:**
```
Build the menu onboarding for Step 3.

Endpoints needed:

1. GET /api/onboarding/menu-template
   → Generate and return Excel file (.xlsx) using Apache POI
   → Template columns: Item Name*, Category*, Price (₹)*, Tax%*, Veg/Non-Veg*, 
                       Description, Available (Y/N)
   → Include sample row: "Paneer Butter Masala, Main Course, 280, 5, Veg, Rich creamy gravy, Y"
   → Include a "Categories" sheet listing all seeded categories

2. POST /api/onboarding/menu-bulk-import
   → Accept .xlsx file upload
   → Parse using Apache POI
   → Validate each row: name required, price > 0, taxSlab must be valid percentage
   → Return preview: { totalRows: 45, validRows: 43, errors: [{row: 12, error: "Price missing"}] }
   → On confirmation (POST /api/onboarding/menu-bulk-import/confirm?importId=xxx):
     → Create all valid MenuItem records
     → Skip errored rows
     → Return summary

3. POST /api/onboarding/menu-quick-add
   → Single item add: name, categoryId, basePrice, taxSlabId, isVeg, description
   → Returns created MenuItem

4. POST /api/onboarding/menu-skip
   → Creates 5 demo items in the "Demo" category
   → Marks step 3 complete

All imports must be idempotent — calling twice should not duplicate items.
Write tests for: valid import, file with errors, empty file, wrong file format (PDF rejected).
```

---

### Step 4 — WhatsApp Business Setup
**URL:** `/onboarding/whatsapp`
**This is the key differentiator for Indian SMBs.**

**What the owner does:**
- Enter their WhatsApp Business phone number
- System sends a test message to verify it works
- Choose which notifications to enable (toggles):
  - Order confirmations to customers
  - Invoice delivery via WhatsApp
  - Low stock alerts to manager
  - Daily sales summary to owner
  - Staff payslip delivery
  - Customer loyalty points update

**Vibe-coding prompt:**
```
Build the WhatsApp Business setup for onboarding Step 4.

This uses Meta's WhatsApp Business Cloud API (not Twilio WhatsApp).
API base: https://graph.facebook.com/v18.0/{phone_number_id}/messages
Auth: Bearer token from WABA (WhatsApp Business Account)

Entities needed:

WhatsAppConfig {
  id UUID,
  businessId UUID,
  phoneNumberId String,       ← Meta's phone_number_id for this business
  wabaId String,              ← WhatsApp Business Account ID  
  accessToken String (encrypted at rest using AES-256),
  verifiedNumber String,      ← The business's WA number e.g. +919876543210
  isActive boolean,
  notifications {
    orderConfirmation: boolean,
    invoiceDelivery: boolean,
    lowStockAlert: boolean,
    dailySalesSummary: boolean,
    payslipDelivery: boolean,
    loyaltyUpdate: boolean
  }
}

WhatsAppTemplate {
  id UUID,
  businessId UUID,
  templateName String,        ← Must match approved template name in Meta
  templateType enum,
  variables String[],         ← Variable names in order e.g. ["customer_name", "order_id"]
  isApproved boolean
}

Endpoints:

POST /api/onboarding/whatsapp/config
  → Save phoneNumberId, wabaId, accessToken
  → Encrypt accessToken before storing (AES-256, key from env)
  → Send test message to business owner's own number:
    "QuickServe is now connected to your WhatsApp Business! 
     Order notifications, invoices and alerts will be sent from this number."
  → Return { connected: true, verifiedNumber: "+91..." }

PUT /api/onboarding/whatsapp/notifications
  → Update notification preferences
  → All default to true on first setup

POST /api/onboarding/whatsapp/skip
  → Mark step 4 complete with WhatsApp disabled
  → Business can enable later from Settings > Integrations

GET /api/onboarding/whatsapp/templates
  → Return list of pre-built templates QuickServe uses
  → Owner must approve these in Meta Business Manager
  → Show status: PENDING_APPROVAL / APPROVED / REJECTED for each

WhatsApp service (WhatsAppService.java):
  sendMessage(String toNumber, String templateName, Map<String, String> variables)
  → Build Meta API request
  → POST to graph.facebook.com
  → Log result (success/failure) to whatsapp_message_log table
  → On failure: retry 3 times with exponential backoff via Kafka retry topic
  → Never throw exception to caller — log and continue

Write tests mocking the Meta API HTTP call.
Write integration test verifying message log is created on send.
```

---

### Step 5 — Go Live
**URL:** `/onboarding/complete`
**What happens:**
- Show summary of what was set up
- Mark Business.status = ACTIVE (was ONBOARDING)
- Redirect to main dashboard
- Send WhatsApp to owner:
  ```
  Your QuickServe ERP is live! 🚀
  
  What you can do now:
  ✓ Take orders at: [pos-url]
  ✓ Customer QR menu: [qr-url]  
  ✓ Kitchen display: [kds-url]
  
  Trial ends: [date] (14 days)
  Upgrade at: [pricing-url]
  
  Need help? Reply to this message.
  ```

**Vibe-coding prompt:**
```
Build the onboarding completion endpoint.

POST /api/onboarding/complete

Requirements:
1. Verify all required steps (0,1,2,3) are complete — step 4 (WhatsApp) is optional
2. Change Business.status from ONBOARDING to ACTIVE
3. Create default roles for this business:
   - OUTLET_MANAGER (can: manage orders, inventory, staff attendance)
   - CASHIER (can: create orders, process payments)
   - WAITER (can: create orders, view tables)
   - KITCHEN_STAFF (can: view and update KDS only)
   - ACCOUNTANT (can: view finance, export reports)
4. Create onboarding completion audit log entry
5. Send WhatsApp go-live message to owner
6. Send welcome email via Resend with:
   - POS link with instructions
   - QR code download link for tables
   - Video tutorial links
   - Support contact
7. Return { dashboardUrl, posUrl, kdsUrl, qrMenuUrl, trialDaysRemaining }

Also build:
GET /api/onboarding/checklist
→ Returns all steps with completion status
→ Frontend shows this as a progress checklist on dashboard until all steps done
→ { steps: [{id, title, completed, url, estimatedMinutes}] }
```

---

## Post-Onboarding First-Time User Experience

After onboarding, show a dismissible "Getting Started" widget on the dashboard with:

1. "Add your first staff member" → /settings/staff/new
2. "Print your table QR codes" → /settings/tables/qr-print
3. "Set up your first customer" → /crm/customers/new  
4. "Take a test order" → /pos (with demo mode toggle)
5. "Connect Razorpay for online payments" → /settings/payments/razorpay
6. "Set stock levels for inventory" → /inventory/items

Each task has a checkbox. Once all 6 are done, widget disappears.
Store completion state in localStorage (not server — it is cosmetic only).

---

## Quick Setup Mode (Sub-5-Minute Onboarding)

For owners who want to start immediately, provide a "Quick Setup" path that:
1. Uses business name as outlet name
2. Creates 10 tables by default
3. Loads demo menu (5 items)
4. Skips WhatsApp setup
5. Goes live immediately

Button on Step 1: "Skip setup, get started now →"
This creates a fully functional (if minimal) account in under 60 seconds.
