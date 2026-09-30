package io.github.krapnuyij.logiops.order.dto;

import io.github.krapnuyij.logiops.order.OutboundOrderItem;

public record OutboundOrderItemResponse(
    Long productId,
    String sku,
    long quantity
) {

  public static OutboundOrderItemResponse from(OutboundOrderItem item) {
    return new OutboundOrderItemResponse(
        item.getProduct().getId(),
        item.getProduct().getSku(),
        item.getQuantity()
    );
  }
}
