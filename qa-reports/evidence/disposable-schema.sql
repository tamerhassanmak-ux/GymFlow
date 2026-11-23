CREATE INDEX idx_check_ins_check_in_time ON check_ins(check_in_time);
CREATE INDEX idx_check_ins_member ON check_ins(member_id);
CREATE INDEX idx_check_ins_status ON check_ins(status);
CREATE INDEX idx_expenses_category
ON expenses(category)
;
CREATE INDEX idx_expenses_date
ON expenses(expense_date)
;
CREATE INDEX idx_expenses_payment_method
ON expenses(payment_method)
;
CREATE INDEX idx_expenses_status
ON expenses(status)
;
CREATE INDEX idx_payments_date ON payments(payment_date);
CREATE INDEX idx_payments_member ON payments(member_id);
CREATE INDEX idx_payments_membership ON payments(membership_id);
CREATE INDEX idx_payments_method ON payments(payment_method);
CREATE INDEX idx_payments_reference ON payments(reference_number);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_revenues_date ON revenues(revenue_date);
CREATE INDEX idx_revenues_name ON revenues(name);
CREATE INDEX idx_revenues_type ON revenues(revenue_type);
CREATE TABLE check_ins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id BIGINT NOT NULL,
    membership_id BIGINT,
    check_in_time VARCHAR(30) NOT NULL,
    check_out_time VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'IN',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id)
        REFERENCES members(id)
        ON DELETE CASCADE,
    FOREIGN KEY (membership_id)
        REFERENCES member_memberships(id)
);
CREATE TABLE expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    expense_code TEXT NOT NULL UNIQUE,

    title TEXT NOT NULL,

    category TEXT NOT NULL,

    amount NUMERIC NOT NULL
        CHECK (amount >= 0),

    expense_date TEXT NOT NULL,

    payment_method TEXT NOT NULL,

    status TEXT NOT NULL DEFAULT 'PAID'
        CHECK (
            status IN (
                'PAID',
                'PENDING',
                'CANCELLED'
            )
        ),

    reference_number TEXT,

    beneficiary TEXT,

    notes TEXT,

    created_at TEXT NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TEXT NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE member_memberships (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id BIGINT NOT NULL,
    membership_plan_id BIGINT NOT NULL,
    start_date VARCHAR(30) NOT NULL,
    end_date VARCHAR(30) NOT NULL,
    price DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    final_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    notes TEXT,
    allowed_sessions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id)
        REFERENCES members(id)
        ON DELETE CASCADE,
    FOREIGN KEY (membership_plan_id)
        REFERENCES membership_plans(id)
);
CREATE TABLE members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_code VARCHAR(30) UNIQUE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    whatsapp VARCHAR(30),
    email VARCHAR(150),
    gender VARCHAR(20),
    birth_date DATE,
    membership VARCHAR(100),
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    notes TEXT,
    photo_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE membership_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_ar VARCHAR(150) NOT NULL,
    name_en VARCHAR(150) NOT NULL,
    duration_months INTEGER NOT NULL,
    price DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    description TEXT,
    max_sessions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE payment_installments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    membership_id INTEGER NOT NULL,
    installment_no INTEGER NOT NULL,
    due_date TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    created_at TEXT,
    updated_at TEXT
);
CREATE TABLE payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id BIGINT NOT NULL,
    membership_id BIGINT,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PAID',
    payment_date VARCHAR(30) NOT NULL,
    reference_number VARCHAR(150),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id)
        REFERENCES members(id)
        ON DELETE RESTRICT,
    FOREIGN KEY (membership_id)
        REFERENCES member_memberships(id)
        ON DELETE SET NULL
);
CREATE TABLE revenue_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE revenues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    revenue_code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    revenue_type TEXT NOT NULL,
    amount NUMERIC NOT NULL CHECK (amount > 0),
    revenue_date TEXT NOT NULL,
    attachment_path TEXT,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE schema_migrations (
    version VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at TEXT NOT NULL
);
CREATE TABLE sqlite_sequence(name,seq);
