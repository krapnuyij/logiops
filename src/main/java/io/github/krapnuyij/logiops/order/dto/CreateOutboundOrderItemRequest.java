package io.github.krapnuyij.logiops.order.dto;

import io.github.krapnuyij.logiops.order.OrderItemCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOutboundOrderItemRequest(
    @NotNull(message = "상품 ID는 필수이다.")
    @Positive(message = "상품 ID는 양수여야 한다.")
    Long productId,
    @NotNull(message = "주문 수량은 필수이다.")
    @Positive(message = "주문 수량은 양수여야 한다.")
    Long quantity
) {

  public OrderItemCommand toCommand() {
    return new OrderItemCommand(productId, quantity);
  }
}
