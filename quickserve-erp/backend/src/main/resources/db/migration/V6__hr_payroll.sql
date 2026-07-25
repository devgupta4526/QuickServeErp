-- V6__hr_payroll.sql
-- Employees, shifts, attendance, leave, payroll

-- ============================
-- EMPLOYEES
-- ============================
CREATE TABLE employees (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    outlet_id        UUID REFERENCES outlets(id),
    user_id          UUID REFERENCES users(id),
    employee_code    VARCHAR(50),
    name             VARCHAR(200) NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(255),
    designation      VARCHAR(100),
    department       VARCHAR(100),
    date_of_joining  DATE,
    date_of_birth    DATE,
    pan_number       VARCHAR(10),
    aadhaar_number_encrypted TEXT, -- AES-256 encrypted
    bank_account     VARCHAR(50),
    ifsc             VARCHAR(20),
    pf_number        VARCHAR(50),
    esi_number       VARCHAR(50),
    employment_type  VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME', -- FULL_TIME, PART_TIME, CONTRACT
    salary           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    salary_type      VARCHAR(20) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, DAILY, HOURLY
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, TERMINATED
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, employee_code)
);

CREATE INDEX idx_employees_business ON employees(business_id);
CREATE INDEX idx_employees_outlet   ON employees(outlet_id);
CREATE INDEX idx_employees_status   ON employees(status);

-- ============================
-- SHIFTS
-- ============================
CREATE TABLE shifts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id      UUID NOT NULL REFERENCES outlets(id),
    name           VARCHAR(100) NOT NULL,  -- Morning, Evening, Night
    start_time     TIME NOT NULL,
    end_time       TIME NOT NULL,
    break_duration INT NOT NULL DEFAULT 30, -- minutes
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE shift_assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    shift_id    UUID NOT NULL REFERENCES shifts(id),
    assigned_date DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(employee_id, assigned_date)
);

CREATE INDEX idx_shift_assign_employee ON shift_assignments(employee_id);
CREATE INDEX idx_shift_assign_date     ON shift_assignments(assigned_date);

-- ============================
-- ATTENDANCE
-- ============================
CREATE TABLE attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employees(id),
    attendance_date DATE NOT NULL,
    check_in        TIMESTAMPTZ,
    check_out       TIMESTAMPTZ,
    work_hours      NUMERIC(5, 2),
    overtime_hours  NUMERIC(5, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PRESENT', -- PRESENT, ABSENT, HALF_DAY, LEAVE
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(employee_id, attendance_date)
);

CREATE INDEX idx_attendance_employee ON attendance(employee_id);
CREATE INDEX idx_attendance_date     ON attendance(attendance_date);

-- ============================
-- LEAVE TYPES
-- ============================
CREATE TABLE leave_types (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id  UUID NOT NULL REFERENCES businesses(id),
    name         VARCHAR(100) NOT NULL,  -- Casual Leave, Sick Leave, Annual Leave
    days_allowed INT NOT NULL DEFAULT 12,
    is_paid      BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================
-- LEAVE APPLICATIONS
-- ============================
CREATE TABLE leave_applications (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id    UUID NOT NULL REFERENCES employees(id),
    leave_type_id  UUID NOT NULL REFERENCES leave_types(id),
    from_date      DATE NOT NULL,
    to_date        DATE NOT NULL,
    reason         TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    approved_by    UUID REFERENCES users(id),
    approved_at    TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leave_app_employee ON leave_applications(employee_id);
CREATE INDEX idx_leave_app_status   ON leave_applications(status);

-- ============================
-- PAYROLL RUNS
-- ============================
CREATE TABLE payrolls (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    month       INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year        INT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PROCESSED, PAID
    processed_by UUID REFERENCES users(id),
    processed_at TIMESTAMPTZ,
    paid_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, month, year)
);

-- ============================
-- PAYROLL SLIPS
-- ============================
CREATE TABLE payroll_slips (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_id           UUID NOT NULL REFERENCES payrolls(id),
    employee_id          UUID NOT NULL REFERENCES employees(id),
    -- Earnings
    basic_salary         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    hra                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    conveyance_allowance NUMERIC(10, 2) NOT NULL DEFAULT 0,
    other_allowances     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    overtime_pay         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    -- Deductions
    pf_employee          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    esi_employee         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tds                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    professional_tax     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    other_deductions     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    -- Computed
    gross_salary         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_deductions     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    net_salary           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    -- Attendance
    working_days         INT NOT NULL DEFAULT 0,
    present_days         INT NOT NULL DEFAULT 0,
    leave_days           INT NOT NULL DEFAULT 0,
    absent_days          INT NOT NULL DEFAULT 0,
    pdf_url              TEXT,
    paid_at              TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(payroll_id, employee_id)
);

CREATE INDEX idx_payroll_slips_payroll   ON payroll_slips(payroll_id);
CREATE INDEX idx_payroll_slips_employee  ON payroll_slips(employee_id);
