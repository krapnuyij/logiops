package io.github.krapnuyij.logiops.order;

import io.github.krapnuyij.logiops.common.error.ConflictException;

public class InvalidOrderStateException extends ConflictException {

  public InvalidOrderStateException(
      Long orderId,
      OutboundOrderStatus currentStatus,
      String action
  ) {
    super(
        "INVALID_ORDER_STATE",
        "현재 상태의 출고 주문에는 요청한 처리를 수행할 수 없다: orderId=" + orderId
            + ", status=" + currentStatus
            + ", action=" + action
    );
  }
}
