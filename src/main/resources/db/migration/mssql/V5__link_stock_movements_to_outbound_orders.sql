ALTER TABLE stock_movements
  ADD outbound_order_id BIGINT NULL;
GO

ALTER TABLE stock_movements
  ADD CONSTRAINT fk_stock_movements_order
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_orders (id);
GO

ALTER TABLE stock_movements
  ADD CONSTRAINT ck_stock_movements_order_reference CHECK (
    CASE
      WHEN movement_type = 'RECEIPT'
        AND outbound_order_id IS NULL THEN 1
      WHEN movement_type <> 'RECEIPT'
        AND outbound_order_id IS NOT NULL THEN 1
      ELSE 0
    END = 1
  );
GO

CREATE INDEX ix_stock_movements_order_occurred
  ON stock_movements (outbound_order_id, occurred_at DESC, id DESC);
