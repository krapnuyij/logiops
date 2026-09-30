package io.github.krapnuyij.logiops.product;

import io.github.krapnuyij.logiops.common.error.ConflictException;

public class DuplicateSkuException extends ConflictException {

  public DuplicateSkuException(String sku) {
    super("DUPLICATE_SKU", "이미 등록된 SKU이다: " + sku);
  }

  public DuplicateSkuException(String sku, Throwable cause) {
    super("DUPLICATE_SKU", "이미 등록된 SKU이다: " + sku, cause);
  }
}
