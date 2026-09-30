package io.github.krapnuyij.logiops.order;

import io.github.krapnuyij.logiops.common.error.NotFoundException;

public class OutboundOrderNotFoundException extends NotFoundException {

  public OutboundOrderNotFoundException(long orderId) {
    super("ORDER_NOT_FOUND", "출고 주문을 찾을 수 없다: " + orderId);
  }
}
