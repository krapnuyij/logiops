package io.github.krapnuyij.logiops.order.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import io.github.krapnuyij.logiops.order.OutboundOrder;
import io.github.krapnuyij.logiops.order.OutboundOrderStatus;

public record OutboundOrderResponse(
    Long id,
    OutboundOrderStatus status,
    List<OutboundOrderItemResponse> items,
    Instant createdAt,
    Instant shippedAt,
    Instant cancelledAt
) {

  public static OutboundOrderResponse from(OutboundOrder outboundOrder) {
    List<OutboundOrderItemResponse> items = outboundOrder.getItems().stream()
        .map(OutboundOrderItemResponse::from)
        .sorted(Comparator.comparingLong(OutboundOrderItemResponse::productId))
        .toList();
    return new OutboundOrderResponse(
        outboundOrder.getId(),
        outboundOrder.getStatus(),
        items,
        outboundOrder.getCreatedAt(),
        outboundOrder.getShippedAt(),
        outboundOrder.getCancelledAt()
    );
  }
}
