package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.common.error.ConflictException;

public class InsufficientStockException extends ConflictException {

  public InsufficientStockException(
      Long productId,
      long requestedQuantity,
      long availableQuantity
  ) {
    super(
        "INSUFFICIENT_STOCK",
        "상품의 가용재고가 부족하다: productId=" + productId
            + ", requested=" + requestedQuantity
            + ", available=" + availableQuantity
    );
  }
}
