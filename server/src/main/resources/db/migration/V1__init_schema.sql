-- Enable JSONB support (standard in Postgres, but good to ensure extensions if needed)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users (Employees/Admins)
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'ADMIN', 'EMPLOYEE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Stores (Inventory Locations)
CREATE TABLE store (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'MASTER', 'VIRTUAL'
    current_owner_user_id BIGINT REFERENCES app_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Products
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL, -- 'BOOK', 'STATIONERY', etc.
    name VARCHAR(255) NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    attributes JSON, -- Flexible attributes (Author, ISBN, etc.)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bundles
CREATE TABLE bundle (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2), -- Optional specific price, or calculated
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE bundle_item (
    bundle_id BIGINT REFERENCES bundle(id),
    product_id BIGINT REFERENCES product(id),
    quantity INT DEFAULT 1,
    PRIMARY KEY (bundle_id, product_id)
);

-- 5. Inventory (Stock Level)
CREATE TABLE stock_level (
    store_id BIGINT REFERENCES store(id),
    product_id BIGINT REFERENCES product(id),
    quantity INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (store_id, product_id)
);

-- 6. Orders
CREATE TABLE customer_order (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT REFERENCES store(id),
    user_id BIGINT REFERENCES app_user(id), -- Employee who processed it
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_line (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES customer_order(id),
    product_id BIGINT REFERENCES product(id),
    bundle_id BIGINT REFERENCES bundle(id), -- Optional, if part of a bundle
    unit_price DECIMAL(10, 2) NOT NULL,
    is_exclusion BOOLEAN DEFAULT FALSE, -- If TRUE, this item was REMOVED from a bundle context (negative logic or tracking)
    quantity INT DEFAULT 1
);

-- 7. Audit Log for Transfers
CREATE TABLE inventory_transfer (
    id BIGSERIAL PRIMARY KEY,
    from_store_id BIGINT REFERENCES store(id),
    to_store_id BIGINT REFERENCES store(id),
    product_id BIGINT REFERENCES product(id),
    quantity INT NOT NULL,
    transferred_by_user_id BIGINT REFERENCES app_user(id),
    transferred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
