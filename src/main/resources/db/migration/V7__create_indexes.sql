CREATE INDEX idx_permissions_operation_id ON permissions(operation_id);
CREATE INDEX idx_permissions_resource_id ON permissions(resource_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_products_category_id ON products(product_category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_warehouses_name ON warehouses(name);
CREATE INDEX idx_warehouse_locations_warehouse_id ON warehouse_locations(warehouse_id);
CREATE INDEX idx_user_warehouses_user_id ON user_warehouses(user_id);
CREATE INDEX idx_user_warehouses_warehouse_id ON user_warehouses(warehouse_id);

CREATE INDEX idx_inbounds_warehouse_status ON inbounds(warehouse_id, status);
CREATE INDEX idx_inbounds_created_by_user_id ON inbounds(created_by_user_id);
CREATE INDEX idx_inbounds_reviewed_by_user_id ON inbounds(reviewed_by_user_id);
CREATE INDEX idx_inbounds_created_at ON inbounds(created_at);
CREATE INDEX idx_inbound_items_inbound_id ON inbound_items(inbound_id);
CREATE INDEX idx_inbound_items_product_id ON inbound_items(product_id);

CREATE INDEX idx_outbounds_warehouse_status ON outbounds(warehouse_id, status);
CREATE INDEX idx_outbounds_created_by_user_id ON outbounds(created_by_user_id);
CREATE INDEX idx_outbounds_reviewed_by_user_id ON outbounds(reviewed_by_user_id);
CREATE INDEX idx_outbounds_created_at ON outbounds(created_at);
CREATE INDEX idx_outbound_items_outbound_id ON outbound_items(outbound_id);
CREATE INDEX idx_outbound_items_product_id ON outbound_items(product_id);
CREATE INDEX idx_outbound_items_location_id ON outbound_items(warehouse_location_id);

CREATE INDEX idx_stock_transfers_warehouse_status ON stock_transfers(warehouse_id, status);
CREATE INDEX idx_stock_transfers_created_by_user_id ON stock_transfers(created_by_user_id);
CREATE INDEX idx_stock_transfers_reviewed_by_user_id ON stock_transfers(reviewed_by_user_id);
CREATE INDEX idx_stock_transfers_created_at ON stock_transfers(created_at);
CREATE INDEX idx_stock_transfer_items_transfer_id ON stock_transfer_items(stock_transfer_id);
CREATE INDEX idx_stock_transfer_items_product_id ON stock_transfer_items(product_id);
CREATE INDEX idx_stock_transfer_items_source_location_id ON stock_transfer_items(source_warehouse_location_id);
CREATE INDEX idx_stock_transfer_items_destination_location_id ON stock_transfer_items(destination_warehouse_location_id);

CREATE INDEX idx_stock_adjustments_warehouse_status ON stock_adjustments(warehouse_id, status);
CREATE INDEX idx_stock_adjustments_warehouse_type ON stock_adjustments(warehouse_id, adjustment_type);
CREATE INDEX idx_stock_adjustments_created_by_user_id ON stock_adjustments(created_by_user_id);
CREATE INDEX idx_stock_adjustments_reviewed_by_user_id ON stock_adjustments(reviewed_by_user_id);
CREATE INDEX idx_stock_adjustments_created_at ON stock_adjustments(created_at);
CREATE INDEX idx_stock_adjustment_items_adjustment_id ON stock_adjustment_items(stock_adjustment_id);
CREATE INDEX idx_stock_adjustment_items_product_id ON stock_adjustment_items(product_id);
CREATE INDEX idx_stock_adjustment_items_location_id ON stock_adjustment_items(warehouse_location_id);

CREATE INDEX idx_location_items_warehouse_product ON warehouse_location_items(warehouse_id, product_id);
CREATE INDEX idx_location_items_product_id ON warehouse_location_items(product_id);
CREATE INDEX idx_stock_movements_warehouse_product_occurred_at ON stock_movements(warehouse_id, product_id, occurred_at);
CREATE INDEX idx_stock_movements_location_product_occurred_at ON stock_movements(warehouse_location_id, product_id, occurred_at);
CREATE INDEX idx_stock_movements_created_by_user_id ON stock_movements(created_by_user_id);
CREATE INDEX idx_stock_movements_source_entity ON stock_movements(source_entity_type, source_entity_id);
CREATE INDEX idx_stock_movements_occurred_at ON stock_movements(occurred_at);

CREATE INDEX idx_audit_trails_user_id ON audit_trails(user_id);
CREATE INDEX idx_audit_trails_entity ON audit_trails(entity_type, entity_id);
CREATE INDEX idx_audit_trails_action ON audit_trails(action);
CREATE INDEX idx_audit_trails_occurred_at ON audit_trails(occurred_at);
