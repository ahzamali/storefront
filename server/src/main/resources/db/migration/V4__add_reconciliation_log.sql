CREATE TABLE reconciliation_log (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES store(id),
    reconciled_by_user_id BIGINT NOT NULL REFERENCES app_user(id),
    total_revenue DECIMAL(19, 2) NOT NULL,
    total_items_sold INT NOT NULL,
    inventory_returned BOOLEAN DEFAULT FALSE,
    details_json TEXT, -- JSON content of sold and returned items
    reconciled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
