package io.github.krapnuyij.logiops.order.dto;

import java.util.List;

import io.github.krapnuyij.logiops.order.OrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOutboundOrderRequest(
    @NotEmpty(message = "출고 주문에는 하나 이상의 항목이 필요하다.")
    List<@NotNull(message = "주문 항목은 null일 수 없다.")
        @Valid CreateOutboundOrderItemRequest> items
) {

  public List<OrderItemCommand> toCommands() {
    return items.stream()
        .map(CreateOutboundOrderItemRequest::toCommand)
        .toList();
  }
}
