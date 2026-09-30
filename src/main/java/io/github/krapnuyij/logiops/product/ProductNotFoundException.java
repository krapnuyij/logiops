package io.github.krapnuyij.logiops.product;

import io.github.krapnuyij.logiops.common.error.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

  public ProductNotFoundException(long productId) {
    super("PRODUCT_NOT_FOUND", "상품을 찾을 수 없다: " + productId);
  }
}
