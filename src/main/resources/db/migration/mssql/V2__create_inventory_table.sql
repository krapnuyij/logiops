CREATE TABLE inventories (
  id BIGINT IDENTITY(1,1) NOT NULL,
  product_id BIGINT NOT NULL,
  on_hand_quantity BIGINT NOT NULL,
  reserved_quantity BIGINT NOT NULL,
  updated_at DATETIME2(6) NOT NULL,
  CONSTRAINT pk_inventories PRIMARY KEY (id),
  CONSTRAINT uk_inventories_product UNIQUE (product_id),
  CONSTRAINT fk_inventories_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT ck_inventories_on_hand_non_negative CHECK (on_hand_quantity >= 0),
  CONSTRAINT ck_inventories_reserved_non_negative CHECK (reserved_quantity >= 0),
  CONSTRAINT ck_inventories_reserved_not_greater_than_on_hand
    CHECK (reserved_quantity <= on_hand_quantity)
);

INSERT INTO inventories (
  product_id,
  on_hand_quantity,
  reserved_quantity,
  updated_at
)
SELECT
  id,
  0,
  0,
  created_at
FROM products;
