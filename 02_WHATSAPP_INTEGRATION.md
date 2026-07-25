# QuickServe ERP — WhatsApp Business Integration
## Complete Specification & Vibe-Coding Prompts

---

## Why WhatsApp First

85% of Indian SMB owners manage their business over WhatsApp.
They share menus, send bills, notify customers — all on WhatsApp.
QuickServe turns this into an automated, professional system.

Key capabilities:
- Customers receive order status on WhatsApp (no app needed)
- Invoices delivered as PDF via WhatsApp
- Owner gets daily P&L summary every morning at 9 AM
- Low stock alerts to manager in real time
- Staff receives payslip on WhatsApp
- Customer receives loyalty points update after every purchase
- Marketing campaigns (birthday offers, re-engagement)

---

## Technical Foundation

### API: Meta WhatsApp Business Cloud API
- No third-party needed — direct Meta integration
- Free for the first 1000 conversations per month per phone number
- Template messages for business-initiated conversations
- Session messages for customer-replied conversations (24-hour window)

### Setup per business (Multi-tenant WhatsApp)
Each business owner must:
1. Create a Meta Business account
2. Create a WhatsApp Business App
3. Get a phone_number_id and WABA (WhatsApp Business Account) ID
4. Generate a permanent access token
5. Submit message templates for approval (takes 0-24 hours usually)

QuickServe provides a guide inside the settings page with screenshots.
For MVP: QuickServe can use a SINGLE shared phone number (with business name in message header).
For production: Each business gets their own number.

---

## Database Schema

```sql
-- V8__whatsapp_notifications.sql

CREATE TABLE whatsapp_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    phone_number_id VARCHAR(50),
    waba_id VARCHAR(50),
    access_token_encrypted TEXT,          -- AES-256 encrypted
    verified_number VARCHAR(20),
    is_active BOOLEAN DEFAULT false,
    -- notification toggles
    notify_order_confirmation BOOLEAN DEFAULT true,
    notify_order_ready BOOLEAN DEFAULT true,
    notify_invoice_delivery BOOLEAN DEFAULT true,
    notify_low_stock BOOLEAN DEFAULT true,
    notify_daily_summary BOOLEAN DEFAULT true,
    notify_payslip BOOLEAN DEFAULT true,
    notify_loyalty_update BOOLEAN DEFAULT true,
    notify_reservation_confirm BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(business_id)
);

CREATE TABLE whatsapp_message_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    to_number VARCHAR(20) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_variables JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, SENT, DELIVERED, READ, FAILED
    wamid VARCHAR(100),                    -- WhatsApp message ID from Meta
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_wa_log_business ON whatsapp_message_log(business_id);
CREATE INDEX idx_wa_log_status ON whatsapp_message_log(status);
CREATE INDEX idx_wa_log_wamid ON whatsapp_message_log(wamid);

CREATE TABLE whatsapp_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50),
    wamid VARCHAR(100),
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT false,
    received_at TIMESTAMPTZ DEFAULT now()
);
```

---

## Message Templates

All templates below are pre-written. Business owner approves them in Meta Business Manager.
Template category is always UTILITY (approved faster than MARKETING).

### Template 1: order_confirmation
**Variables:** {{1}}=customer_name, {{2}}=order_number, {{3}}=outlet_name, {{4}}=items_summary, {{5}}=total_amount, {{6}}=tracking_link

```
Namaste {{1}}! 🙏

Your order has been confirmed at {{3}}.

Order #{{2}}
{{4}}

Total: ₹{{5}}

Track your order: {{6}}

Thank you for dining with us!
```

### Template 2: order_ready
**Variables:** {{1}}=customer_name, {{2}}=order_number, {{3}}=outlet_name

```
{{1}}, your order #{{2}} is ready! 🍽️

Please collect from {{3}}.

Thank you!
```

### Template 3: invoice_delivery
**Variables:** {{1}}=customer_name, {{2}}=invoice_number, {{3}}=amount, {{4}}=business_name

**Header:** Document (PDF attachment)

```
Dear {{1}},

Please find your invoice #{{2}} attached.

Amount: ₹{{3}}
Business: {{4}}

For any queries, please reply to this message.
```

### Template 4: daily_sales_summary
**Variables:** {{1}}=owner_name, {{2}}=date, {{3}}=total_orders, {{4}}=total_revenue, {{5}}=top_item, {{6}}=cash_amount, {{7}}=upi_amount, {{8}}=online_amount

```
Good morning {{1}}! ☀️

Your sales summary for {{2}}:

📊 Total Orders: {{3}}
💰 Revenue: ₹{{4}}
🏆 Top Item: {{5}}

Payment breakdown:
💵 Cash: ₹{{6}}
📱 UPI: ₹{{7}}
💳 Online: ₹{{8}}

Have a great day!
— QuickServe
```

### Template 5: low_stock_alert
**Variables:** {{1}}=manager_name, {{2}}=item_name, {{3}}=current_stock, {{4}}=unit, {{5}}=reorder_point, {{6}}=supplier_name

```
⚠️ Low Stock Alert

Hi {{1}},

*{{2}}* is running low.

Current stock: {{3}} {{4}}
Reorder point: {{5}} {{4}}
Suggested supplier: {{6}}

Please place a purchase order soon.

— QuickServe ERP
```

### Template 6: payslip_delivery
**Variables:** {{1}}=employee_name, {{2}}=month_year, {{3}}=net_salary, {{4}}=business_name

**Header:** Document (PDF attachment)

```
Dear {{1}},

Your payslip for {{2}} is attached.

Net Salary: ₹{{3}}

For queries, contact HR at {{4}}.

Thank you for your service!
```

### Template 7: loyalty_points_update
**Variables:** {{1}}=customer_name, {{2}}=points_earned, {{3}}=total_points, {{4}}=tier, {{5}}=business_name

```
{{1}}, you earned {{2}} points! 🎉

Total points: {{3}}
Current tier: {{4}} ⭐

Visit us again to earn more points at {{5}}.
```

### Template 8: reservation_confirmation
**Variables:** {{1}}=guest_name, {{2}}=outlet_name, {{3}}=date, {{4}}=time, {{5}}=party_size, {{6}}=table_name

```
Reservation Confirmed! ✅

Hi {{1}},

Your table is booked at {{2}}.

📅 Date: {{3}}
🕐 Time: {{4}}
👥 Party of: {{5}}
🪑 Table: {{6}}

See you soon! Reply if you need to modify.
```

### Template 9: birthday_offer (Marketing)
**Variables:** {{1}}=customer_name, {{2}}=discount, {{3}}=business_name, {{4}}=valid_until

```
Happy Birthday {{1}}! 🎂🎉

{{3}} wishes you a wonderful birthday!

As our special gift to you:
🎁 {{2}}% off on your next visit
Valid till: {{4}}

Show this message to avail the offer.
```

### Template 10: winback_campaign (Marketing)
**Variables:** {{1}}=customer_name, {{2}}=days_since_visit, {{3}}=business_name, {{4}}=offer

```
We miss you, {{1}}! 😊

It's been {{2}} days since your last visit to {{3}}.

We'd love to have you back!
Special offer just for you: {{4}}

Visit us soon — this offer expires in 7 days.
```

---

## Vibe-Coding Prompt: WhatsApp Service (Core)

```
Build the complete WhatsApp Business integration for QuickServe ERP.

File: src/main/java/com/quickserve/modules/whatsapp/WhatsAppService.java

The service must:

1. SEND TEXT TEMPLATE MESSAGE
   Method: sendTemplateMessage(String businessId, String toNumber, String templateName, List<String> variables)
   
   - Load WhatsAppConfig for businessId from cache (Redis, TTL 1 hour) then DB
   - If config not found or inactive: log warning, return false, don't throw
   - Decrypt access token using AES-256 (key from application.yml: whatsapp.encryption-key)
   - Build Meta API request:
     POST https://graph.facebook.com/v18.0/{phone_number_id}/messages
     Authorization: Bearer {access_token}
     Content-Type: application/json
     
     Body:
     {
       "messaging_product": "whatsapp",
       "to": "91{mobile_number}",   ← always prefix with 91 for India
       "type": "template",
       "template": {
         "name": "{templateName}",
         "language": { "code": "en" },
         "components": [{
           "type": "body",
           "parameters": [
             {"type": "text", "text": "value1"},
             {"type": "text", "text": "value2"}
           ]
         }]
       }
     }
   
   - Use Spring WebClient (non-blocking)
   - On success (200): save to whatsapp_message_log with status=SENT, wamid from response
   - On 4xx error: log error, save to log with status=FAILED, do NOT retry
   - On 5xx/timeout: publish to Kafka topic "whatsapp.retry" for retry handling
   - Return boolean (true=sent, false=failed)

2. SEND DOCUMENT (PDF) MESSAGE
   Method: sendDocument(String businessId, String toNumber, String templateName, 
                        List<String> variables, String documentUrl, String filename)
   
   Same as above but template component includes header with document:
   {
     "type": "header",
     "parameters": [{
       "type": "document",
       "document": {
         "link": "{documentUrl}",
         "filename": "{filename}"
       }
     }]
   }
   
   Note: documentUrl must be publicly accessible (MinIO presigned URL with 72-hour expiry)

3. WEBHOOK HANDLER (for delivery receipts)
   POST /api/webhooks/whatsapp
   GET  /api/webhooks/whatsapp  ← Meta sends verification challenge here
   
   GET handler:
   - Read query params: hub.mode, hub.verify_token, hub.challenge
   - Verify hub.verify_token matches env var WHATSAPP_WEBHOOK_VERIFY_TOKEN
   - Return hub.challenge as plain text (200)
   
   POST handler:
   - Parse Meta webhook payload
   - For status updates (delivered, read, failed): update whatsapp_message_log.status
   - For incoming customer messages: publish to Kafka "whatsapp.incoming" topic
   - Always return 200 immediately (Meta will retry if you return non-200)
   - Save raw payload to whatsapp_webhook_events for debugging

4. RETRY CONSUMER
   @KafkaListener(topics = "whatsapp.retry")
   - Retry failed messages up to 3 times with exponential backoff
   - After 3 failures: mark log as PERMANENTLY_FAILED, alert admin via email

5. INCOMING MESSAGE HANDLER
   @KafkaListener(topics = "whatsapp.incoming")
   - Parse customer message
   - Simple keyword handling:
     "ORDER STATUS" → reply with last order status for that customer number
     "MENU" → reply with menu link
     "HELP" → reply with support message
     "STOP" → mark customer as opted-out of WhatsApp messages
   - Store opt-out in customer.whatsappOptOut = true

Write complete implementation with all imports.
Use @Slf4j for logging.
Include AES encryption/decryption utility class: WhatsAppTokenEncryptor.java
Write unit tests mocking WebClient for all 4 cases (success, 4xx, 5xx, timeout).
```

---

## Vibe-Coding Prompt: WhatsApp Kafka Event Listeners

```
Build the Kafka consumers that trigger WhatsApp messages for QuickServe ERP.

These consumers listen to business events and send appropriate WhatsApp messages.

FILE 1: OrderWhatsAppListener.java
@KafkaListener(topics = "order.status.changed")

Payload: { orderId, businessId, customerId, newStatus, orderNumber, outletName }

Logic:
- Load customer phone from CRM
- If customer.whatsappOptOut == true: skip
- Load WhatsAppConfig for businessId: if notify_order_confirmation == false: skip

On newStatus == "PLACED":
  → Send template "order_confirmation"
  → Variables: [customerName, orderNumber, outletName, itemsSummary, totalAmount, trackingLink]
  → trackingLink = https://app.quickserve.in/track/{orderId}
  → itemsSummary: "Paneer Butter Masala x2, Roti x4" (max 100 chars, truncate)

On newStatus == "READY":
  → Send template "order_ready"
  → Variables: [customerName, orderNumber, outletName]

On newStatus == "DELIVERED":
  → If loyalty program enabled for business:
    → Calculate points earned (load LoyaltyProgram config)
    → Add points to customer account
    → Send template "loyalty_points_update"


FILE 2: FinanceWhatsAppListener.java
@KafkaListener(topics = "invoice.generated")

Payload: { invoiceId, businessId, customerId, invoiceNumber, amount, pdfUrl }

Logic:
- Load customer phone
- Load WhatsAppConfig: check notify_invoice_delivery
- Get presigned URL for PDF (72 hours) from MinIO
- Send document template "invoice_delivery"
- Variables: [customerName, invoiceNumber, formattedAmount, businessName]
- Attach PDF


FILE 3: InventoryWhatsAppListener.java
@KafkaListener(topics = "stock.low")

Payload: { businessId, outletId, itemId, itemName, currentStock, unit, reorderPoint }

Logic:
- Load outlet manager phone for this outletId
- Load WhatsAppConfig: check notify_low_stock
- Load supplier name from inventory item
- Send template "low_stock_alert"
- Do NOT send duplicate alerts: check Redis key "low_stock_alert:{itemId}" 
  If key exists (TTL 6 hours): skip sending
  If key not exists: send + set Redis key with TTL 6 hours


FILE 4: DailySummaryScheduler.java
@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")   ← Every day 9 AM IST

Logic:
- Get all ACTIVE businesses with notify_daily_summary == true
- For each business: 
  - Calculate yesterday's stats:
    - total_orders: COUNT orders WHERE date = yesterday AND businessId
    - total_revenue: SUM payments WHERE date = yesterday AND businessId
    - top_item: most ordered item by quantity yesterday
    - payment breakdown: GROUP BY payment method
  - Load business owner phone
  - Send template "daily_sales_summary"
  - If no orders yesterday: skip (don't send "₹0 revenue" — that's demoralizing)


FILE 5: HRWhatsAppListener.java
@KafkaListener(topics = "payroll.processed")

Payload: { payrollId, businessId, employeeId, employeeName, netSalary, monthYear, payslipPdfUrl }

Logic:
- Load employee phone from HR module
- Check if employee has phone number (some might not)
- Get presigned URL for payslip PDF
- Send document template "payslip_delivery"

Write all 5 files completely.
Include proper @Transactional(readOnly = true) where only reading.
Include proper error handling — one failed WhatsApp send must not stop batch processing.
Write tests for DailySummaryScheduler with mocked business data and mocked WhatsApp service.
```

---

## Vibe-Coding Prompt: WhatsApp Campaign Manager

```
Build the WhatsApp campaign manager for QuickServe ERP.
This allows business owners to send bulk WhatsApp messages to customer segments.

Entity: Campaign
{
  id UUID,
  businessId UUID,
  name String,
  type: BIRTHDAY / ANNIVERSARY / WINBACK / PROMOTIONAL / CUSTOM,
  templateName String,
  targetSegment: ALL / GOLD_TIER / SILVER_TIER / INACTIVE_30D / INACTIVE_60D / BIRTHDAY_THIS_WEEK,
  scheduledAt TIMESTAMPTZ,  ← null = send immediately
  status: DRAFT / SCHEDULED / RUNNING / COMPLETED / CANCELLED,
  totalTargets INT,
  sentCount INT,
  deliveredCount INT,
  readCount INT,
  createdAt TIMESTAMPTZ
}

Endpoints:

POST /api/crm/campaigns
→ Create campaign (status=DRAFT)
→ Estimate audience size based on targetSegment
→ Return { campaignId, estimatedAudience: 234 }

POST /api/crm/campaigns/{id}/schedule
→ Set scheduledAt and change status to SCHEDULED

POST /api/crm/campaigns/{id}/send-now
→ Change status to RUNNING
→ Publish "campaign.start" Kafka event
→ Return immediately (don't wait for sends)

GET /api/crm/campaigns/{id}/stats
→ Return live stats from whatsapp_message_log:
   { totalTargets, sentCount, deliveredCount, readCount, failedCount, deliveryRate% }

Campaign Executor (Kafka Consumer):
@KafkaListener(topics = "campaign.start")
→ Load campaign
→ Query customers based on targetSegment:
  - INACTIVE_30D: customers where lastVisitAt < 30 days ago AND visitCount > 0
  - BIRTHDAY_THIS_WEEK: customers where EXTRACT(month,dob)=current_month AND EXTRACT(day,dob) BETWEEN today AND today+7
→ Rate limit sends: max 50 messages per minute (Meta limit for standard accounts)
→ Use Redis counter with 1-minute TTL window
→ For each customer: publish individual "whatsapp.send" event
→ Update campaign.sentCount in real time

Scheduled Campaign Runner:
@Scheduled(fixedDelay = 60000)  ← check every minute
→ Find campaigns WHERE status=SCHEDULED AND scheduledAt <= now()
→ Trigger them

Rate limiting implementation:
- Redis key: "wa_rate:{phoneNumberId}:{minute_timestamp}"
- Increment on each send, expire after 90 seconds
- If count > 50: wait until next minute window

Write complete implementation with tests.
```

---

## WhatsApp Opt-Out Compliance

Indian regulations and Meta policies require proper opt-out handling.

```
Rules to implement:

1. Every WhatsApp message must include opt-out footer:
   "Reply STOP to unsubscribe from messages"
   (Add as footer component in all templates)

2. When customer replies "STOP", "UNSUBSCRIBE", "OPT OUT", or similar:
   → Set customer.whatsappOptOut = true
   → Send ONE final message: "You have been unsubscribed from [Business] messages."
   → Never send marketing messages to opted-out customers
   → Order confirmations and invoices can still be sent (transactional)

3. Re-subscribe: customer sends "START" or "SUBSCRIBE"
   → Set customer.whatsappOptOut = false

4. Log all opt-outs in audit log

Build the opt-out handler as part of the WhatsApp incoming message processor.
```
