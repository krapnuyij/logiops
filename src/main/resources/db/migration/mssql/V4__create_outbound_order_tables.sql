CREATE TABLE outbound_orders (
  id BIGINT IDENTITY(1,1) NOT NULL,
  status NVARCHAR(16) NOT NULL,
  created_at DATETIME2(6) NOT NULL,
  shipped_at DATETIME2(6) NULL,
  cancelled_at DATETIME2(6) NULL,
  CONSTRAINT pk_outbound_orders PRIMARY KEY (id),
  CONSTRAINT ck_outbound_orders_status CHECK (
    CASE
      WHEN status = 'RESERVED' THEN 1
      WHEN status = 'SHIPPED' THEN 1
      WHEN status = 'CANCELLED' THEN 1
      ELSE 0
    END = 1
  ),
  CONSTRAINT ck_outbound_orders_status_time CHECK (
    CASE
      WHEN status = 'RESERVED'
        AND shipped_at IS NULL
        AND cancelled_at IS NULL THEN 1
      WHEN status = 'SHIPPED'
        AND shipped_at IS NOT NULL
        AND cancelled_at IS NULL THEN 1
      WHEN status = 'CANCELLED'
        AND shipped_at IS NULL
        AND cancelled_at IS NOT NULL THEN 1
      ELSE 0
    END = 1
  )
);

CREATE TABLE outbound_order_items (
  id BIGINT IDENTITY(1,1) NOT NULL,
  outbound_order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity BIGINT NOT NULL,
  CONSTRAINT pk_outbound_order_items PRIMARY KEY (id),
  CONSTRAINT fk_outbound_order_items_order
    FOREIGN KEY (outbound_order_id) REFERENCES outbound_orders (id),
  CONSTRAINT fk_outbound_order_items_product
    FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT uk_outbound_order_items_order_product
    UNIQUE (outbound_order_id, product_id),
  CONSTRAINT ck_outbound_order_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_outbound_order_items_product
  ON outbound_order_items (product_id);
