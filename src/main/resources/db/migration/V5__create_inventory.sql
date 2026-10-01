CREATE TABLE stock_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    notes TEXT,
    rejection_note TEXT,
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    reviewed_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE stock_transfer_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_transfer_id UUID NOT NULL REFERENCES stock_transfers(id),
    product_id UUID NOT NULL REFERENCES products(id),
    source_warehouse_location_id UUID NOT NULL REFERENCES warehouse_locations(id),
    destination_warehouse_location_id UUID NOT NULL REFERENCES warehouse_locations(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_stock_transfer_items_distinct_locations CHECK (source_warehouse_location_id <> destination_warehouse_location_id),
    CONSTRAINT uq_stock_transfer_items_transfer_product UNIQUE (stock_transfer_id, product_id)
);

CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    adjustment_type VARCHAR(30) NOT NULL CHECK (adjustment_type IN ('STOCK_OPNAME', 'DAMAGED_GOODS', 'LOST_GOODS', 'CORRECTION')),
    reason TEXT NOT NULL,
    rejection_note TEXT,
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    reviewed_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE stock_adjustment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_adjustment_id UUID NOT NULL REFERENCES stock_adjustments(id),
    product_id UUID NOT NULL REFERENCES products(id),
    warehouse_location_id UUID NOT NULL REFERENCES warehouse_locations(id),
    quantity_change INTEGER NOT NULL CHECK (quantity_change <> 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_stock_adjustment_items_adjustment_product UNIQUE (stock_adjustment_id, product_id)
);

CREATE TABLE warehouse_location_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    warehouse_location_id UUID NOT NULL REFERENCES warehouse_locations(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_warehouse_location_items_location_product UNIQUE (warehouse_location_id, product_id)
);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    warehouse_location_id UUID NOT NULL REFERENCES warehouse_locations(id),
    product_id UUID NOT NULL REFERENCES products(id),
    movement_type VARCHAR(30) NOT NULL CHECK (movement_type IN ('INBOUND', 'OUTBOUND', 'STOCK_TRANSFER', 'STOCK_ADJUSTMENT')),
    movement_direction VARCHAR(3) NOT NULL CHECK (movement_direction IN ('IN', 'OUT')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    stock_after INTEGER NOT NULL CHECK (stock_after >= 0),
    source_entity_type VARCHAR(30) NOT NULL CHECK (source_entity_type IN ('INBOUND', 'OUTBOUND', 'STOCK_TRANSFER', 'STOCK_ADJUSTMENT')),
    source_entity_id UUID NOT NULL,
    source_item_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);
