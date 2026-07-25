-- V4__finance.sql
-- Double-entry accounting, invoices, expenses, bank accounts

-- ============================
-- CHART OF ACCOUNTS
-- ============================
CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(20) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    parent_id   UUID REFERENCES accounts(id),
    balance     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    is_system   BOOLEAN NOT NULL DEFAULT false,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, code)
);

CREATE INDEX idx_accounts_business ON accounts(business_id);
CREATE INDEX idx_accounts_type     ON accounts(type);

-- ============================
-- JOURNAL ENTRIES (double-entry bookkeeping)
-- ============================
CREATE TABLE journal_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES businesses(id),
    reference     VARCHAR(100),
    description   TEXT NOT NULL,
    entry_date    DATE NOT NULL,
    total_debit   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_credit  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    type          VARCHAR(50),   -- ORDER_PAYMENT, PURCHASE, EXPENSE, PAYROLL, etc.
    source_module VARCHAR(50),
    source_id     UUID,
    created_by    UUID REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_je_business   ON journal_entries(business_id);
CREATE INDEX idx_je_date       ON journal_entries(entry_date);
CREATE INDEX idx_je_source     ON journal_entries(source_module, source_id);

-- ============================
-- JOURNAL ENTRY LINES
-- ============================
CREATE TABLE journal_lines (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id   UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id   UUID NOT NULL REFERENCES accounts(id),
    debit        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    description  TEXT
);

CREATE INDEX idx_jl_journal  ON journal_lines(journal_id);
CREATE INDEX idx_jl_account  ON journal_lines(account_id);

-- ============================
-- INVOICES
-- ============================
CREATE TABLE invoices (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    outlet_id        UUID REFERENCES outlets(id),
    invoice_number   VARCHAR(50) NOT NULL,
    customer_id      UUID REFERENCES customers(id),
    order_id         UUID REFERENCES orders(id),
    type             VARCHAR(20) NOT NULL DEFAULT 'SALES', -- SALES, PURCHASE, CREDIT_NOTE, DEBIT_NOTE
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT, PAID, OVERDUE, CANCELLED
    issue_date       DATE NOT NULL,
    due_date         DATE,
    subtotal         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    cgst             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    sgst             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    igst             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    irn              VARCHAR(100),   -- e-Invoice IRN
    qr_code          TEXT,           -- e-Invoice QR
    e_invoice_status VARCHAR(20),    -- PENDING, GENERATED, FAILED
    pdf_url          TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, invoice_number)
);

CREATE INDEX idx_invoices_business  ON invoices(business_id);
CREATE INDEX idx_invoices_customer  ON invoices(customer_id);
CREATE INDEX idx_invoices_order     ON invoices(order_id);
CREATE INDEX idx_invoices_status    ON invoices(status);
CREATE INDEX idx_invoices_date      ON invoices(issue_date);

-- ============================
-- INVOICE LINE ITEMS
-- ============================
CREATE TABLE invoice_line_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description   VARCHAR(255) NOT NULL,
    hsn_code      VARCHAR(10),
    quantity      NUMERIC(10, 3) NOT NULL DEFAULT 1,
    unit_price    NUMERIC(10, 2) NOT NULL,
    tax_slab_id   UUID REFERENCES tax_slabs(id),
    taxable_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    cgst          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    sgst          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    igst          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total         NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_line_items(invoice_id);

-- ============================
-- EXPENSES
-- ============================
CREATE TABLE expenses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   UUID NOT NULL REFERENCES businesses(id),
    outlet_id     UUID REFERENCES outlets(id),
    category      VARCHAR(100) NOT NULL,
    description   TEXT NOT NULL,
    amount        NUMERIC(10, 2) NOT NULL,
    tax_amount    NUMERIC(10, 2) NOT NULL DEFAULT 0,
    vendor_id     UUID,
    receipt_url   TEXT,
    expense_date  DATE NOT NULL,
    approved_by   UUID REFERENCES users(id),
    created_by    UUID REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_business ON expenses(business_id);
CREATE INDEX idx_expenses_date     ON expenses(expense_date);

-- ============================
-- BANK ACCOUNTS
-- ============================
CREATE TABLE bank_accounts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id    UUID NOT NULL REFERENCES businesses(id),
    account_number VARCHAR(50) NOT NULL,
    ifsc           VARCHAR(20),
    bank_name      VARCHAR(100) NOT NULL,
    account_type   VARCHAR(20) NOT NULL DEFAULT 'CURRENT', -- CURRENT, SAVINGS
    balance        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bank_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_account_id  UUID NOT NULL REFERENCES bank_accounts(id),
    type             VARCHAR(10) NOT NULL, -- CREDIT, DEBIT
    amount           NUMERIC(15, 2) NOT NULL,
    description      TEXT,
    reference        VARCHAR(100),
    transaction_date DATE NOT NULL,
    reconciled_with  UUID,
    is_reconciled    BOOLEAN NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bank_txn_account ON bank_transactions(bank_account_id);
CREATE INDEX idx_bank_txn_date    ON bank_transactions(transaction_date);
