package io.github.krapnuyij.logiops.order;

import io.github.krapnuyij.logiops.common.error.ValidationException;

public class DuplicateOrderItemException extends ValidationException {

  public DuplicateOrderItemException(Long productId) {
    super("DUPLICATE_ORDER_ITEM", "출고 주문에 같은 상품을 중복 지정할 수 없다: " + productId);
  }
}
