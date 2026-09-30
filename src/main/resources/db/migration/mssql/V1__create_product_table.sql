CREATE TABLE products (
  id BIGINT IDENTITY(1,1) NOT NULL,
  sku NVARCHAR(64) NOT NULL,
  name NVARCHAR(100) NOT NULL,
  created_at DATETIME2(6) NOT NULL,
  CONSTRAINT pk_products PRIMARY KEY (id),
  CONSTRAINT uk_products_sku UNIQUE (sku)
);
